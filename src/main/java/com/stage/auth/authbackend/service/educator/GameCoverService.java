package com.stage.auth.authbackend.service.educator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stage.auth.authbackend.dto.game.GenerateCoverPreviewRequest;
import com.stage.auth.authbackend.entity.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

@Service
@RequiredArgsConstructor
@Slf4j
public class GameCoverService {
    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";
    private static final int MAX_PROMPT_LENGTH = 160;

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.gemini.api-key:}")
    private String apiKey;

    @Value("${ai.gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${ai.gemini.enabled:false}")
    private boolean enabled;

    @Value("${ai.gemini.timeout-ms:10000}")
    private int timeoutMs;

    public String generateCoverUrl(Jeu jeu) {
        configureTimeouts();
        String prompt = buildDirectImagePrompt(jeu);
        String refined = refinePromptWithGemini(prompt);
        String externalUrl = buildPollinationsUrl(refined, jeu.getTitre(), jeu.getDescription());
        String asData = tryFetchDataUrl(externalUrl);
        return asData != null ? asData : externalUrl;
    }

    public String generateCoverUrlForPreview(GenerateCoverPreviewRequest request) {
        Jeu draft = Jeu.builder()
                .titre(request.getTitre())
                .description(request.getDescription())
                .typeJeu(request.getTypeJeu() == null ? TypeJeu.QUIZ : request.getTypeJeu())
                .ageMin(request.getAgeMin())
                .ageMax(request.getAgeMax())
                .difficulte(request.getDifficulte())
                .modeJeu(ModeJeu.INDIVIDUEL)
                .build();
        return generateCoverUrl(draft);
    }

    private String tryFetchDataUrl(String externalUrl) {
        int attempts = 2;
        for (int i = 0; i < attempts; i++) {
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.set(HttpHeaders.ACCEPT, "image/*");
                headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0");
                ResponseEntity<byte[]> res = restTemplate.exchange(
                        externalUrl,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        byte[].class
                );
                byte[] body = res.getBody();
                if (body == null || body.length == 0) {
                    continue;
                }
                MediaType contentType = res.getHeaders().getContentType();
                String mime = (contentType != null && contentType.toString().startsWith("image/"))
                        ? contentType.toString()
                        : "image/jpeg";
                return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(body);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    private String refinePromptWithGemini(String draftPrompt) {
        if (!enabled || apiKey == null || apiKey.isBlank()) return draftPrompt;
        int attempts = 2;
        Exception lastError = null;
        for (int i = 0; i < attempts; i++) {
            try {
                return callGeminiForPrompt(draftPrompt);
            } catch (Exception e) {
                lastError = e;
            }
        }
        if (lastError != null) {
            log.warn("Cover prompt refine failed, using local prompt: {}", lastError.getMessage());
        }
        return draftPrompt;
    }

    private String callGeminiForPrompt(String draftPrompt) {
        try {
            String instruction = """
                    Improve this image prompt for a premium educational game cover.
                    Keep it concise and visual.
                    Return ONLY the improved prompt text.
                    Prompt:
                    %s
                    """.formatted(draftPrompt);

            String url = GEMINI_BASE_URL + "/" + model + ":generateContent?key=" + apiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", instruction))))
            );
            HttpEntity<Map<String, Object>> req = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, req, String.class);
            String raw = response.getBody();
            if (raw == null || raw.isBlank()) return draftPrompt;
            JsonNode root = objectMapper.readTree(raw);
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            String cleaned = clean(text, MAX_PROMPT_LENGTH);
            return cleaned.isBlank() ? draftPrompt : cleaned;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void configureTimeouts() {
        if (restTemplate.getRequestFactory() instanceof SimpleClientHttpRequestFactory factory) {
            factory.setConnectTimeout(timeoutMs);
            factory.setReadTimeout(timeoutMs);
            return;
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        restTemplate.setRequestFactory(factory);
    }

    private String buildDirectImagePrompt(Jeu jeu) {
        String type = jeu.getTypeJeu() != null ? jeu.getTypeJeu().name() : "GAME";
        String title = clean(safe(jeu.getTitre(), "Educational game"), 42);
        String description = clean(safe(jeu.getDescription(), "interactive learning challenge"), 90);
        String age = (jeu.getAgeMin() != null ? jeu.getAgeMin() : "?") + "-" + (jeu.getAgeMax() != null ? jeu.getAgeMax() : "?");

        String typeScene = switch (type) {
            case "QUIZ" -> "quiz challenge classroom scene";
            case "MEMOIRE" -> "memory matching pairs scene";
            case "LOGIQUE" -> "logic puzzle reasoning scene";
            case "REFLEXE" -> "fast reaction timing scene";
            default -> "educational game scene";
        };

        return clean(String.join(", ",
                "cinematic premium educational game cover",
                typeScene,
                "title " + title,
                "description " + description,
                "age range " + age,
                "game type " + type,
                "modern lighting",
                "16:9",
                "no text, no logo, no watermark"), MAX_PROMPT_LENGTH);
    }

    private String buildPollinationsUrl(String prompt, String title, String description) {
        String compactPrompt = clean(prompt, MAX_PROMPT_LENGTH);
        if (compactPrompt.length() > 140) {
            compactPrompt = clean(compactPrompt, 140);
        }
        int seed = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        long nonce = System.currentTimeMillis();
        String encoded = URLEncoder.encode(compactPrompt, StandardCharsets.UTF_8).replace("+", "%20");
        if (encoded.length() > 900) {
            compactPrompt = clean(compactPrompt, 110);
            encoded = URLEncoder.encode(compactPrompt, StandardCharsets.UTF_8).replace("+", "%20");
        }
        return "https://image.pollinations.ai/prompt/" + encoded
                + "?width=1280&height=720&model=flux&nologo=true&enhance=true&seed=" + seed + "&t=" + nonce;
    }

    private String safe(String value, String fallback) {
        return notBlank(value) ? value.trim() : fallback;
    }

    private boolean notBlank(String value) {
        return value != null && !value.isBlank();
    }

    private String clean(String value, int maxLength) {
        if (value == null) return "";
        String out = value
                .replaceAll("[\\r\\n\\t]+", " ")
                .replaceAll("[|`\"'<>\\[\\]{}]", " ")
                .replaceAll("\\s+", " ")
                .trim();
        if (out.length() > maxLength) out = out.substring(0, maxLength).trim();
        return out;
    }
}

