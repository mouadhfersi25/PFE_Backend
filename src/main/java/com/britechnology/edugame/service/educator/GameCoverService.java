package com.britechnology.edugame.service.educator;

import com.britechnology.edugame.entity.Jeu;
import com.britechnology.edugame.entity.ModeJeu;
import com.britechnology.edugame.entity.TypeJeu;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.britechnology.edugame.dto.game.GenerateCoverPreviewRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadLocalRandom;

/**
 * Couvertures : Gemini Image (clé {@code GEMINI_API_KEY}) puis Pollinations puis SVG.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class GameCoverService {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    private static final int POLLINATIONS_WIDTH = 800;
    private static final int POLLINATIONS_HEIGHT = 450;

    private static final int POLLINATIONS_CONNECT_MS = 1_200;
    private static final int POLLINATIONS_READ_MS = 3_500;

    private static final int MIN_IMAGE_BYTES = 512;
    private static final int MAX_IMAGE_BYTES = 2_500_000;

    private final ObjectMapper objectMapper;

    @Value("${ai.gemini.api-key:}")
    private String geminiApiKey;

    @Value("${ai.cover.gemini-enabled:true}")
    private boolean coverGeminiEnabled;

    /**
     * Liste de modèles image (ordre = priorité). Défaut = un seul modèle pour limiter la latence max.
     * Voir https://ai.google.dev/gemini-api/docs/image-generation
     */
    @Value("${ai.cover.gemini-models:gemini-2.5-flash-image}")
    private String coverGeminiModelsCsv;

    @Value("${ai.cover.connect-timeout-ms:1500}")
    private int geminiConnectTimeoutMs;

    /** Budget lecture par tentative (plusieurs modèles possibles) */
    @Value("${ai.cover.read-timeout-ms:4800}")
    private int geminiReadTimeoutMs;

    /** Réessaie après 429 / timeout / 503 (réduit pour limiter le temps d’attente max). */
    @Value("${ai.cover.gemini-max-retries:2}")
    private int geminiMaxRetries;

    private final RestTemplate pollinationsRestTemplate = createPollinationsRestTemplate();

    private RestTemplate createPollinationsRestTemplate() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(POLLINATIONS_CONNECT_MS);
        factory.setReadTimeout(POLLINATIONS_READ_MS);
        return new RestTemplate(factory);
    }

    private RestTemplate createGeminiRestTemplate(int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Math.max(500, geminiConnectTimeoutMs));
        factory.setReadTimeout(Math.max(500, readTimeoutMs));
        return new RestTemplate(factory);
    }

    public String generateCoverImageDataUrl(Jeu jeu) {
        if (shouldUseGeminiCover()) {
            List<String> models = parseModelList(coverGeminiModelsCsv);
            if (!models.isEmpty()) {
                for (int i = 0; i < models.size(); i++) {
                    if (i > 0) {
                        sleepQuiet(200);
                    }
                    String model = models.get(i);
                    // 1er modèle = budget principal ; suivants un peu plus courts pour limiter la série
                    int readMs = (i == 0) ? geminiReadTimeoutMs : Math.min(3_500, Math.max(2_200, geminiReadTimeoutMs / 2));
                    String fromGemini = tryGeminiModelsVariants(jeu, model, readMs);
                    if (fromGemini != null) {
                        return fromGemini;
                    }
                }
            }
            log.warn("Cover Gemini: aucun modèle n'a renvoyé d'image — essai Pollinations.");
        }

        return generateViaPollinationsThenSvg(jeu);
    }

    /**
     * Pour un modèle : essai avec imageConfig 16:9, puis sans (certains modèles rejettent le champ).
     */
    private String tryGeminiModelsVariants(Jeu jeu, String model, int readTimeoutMs) {
        String with = callGeminiImageApi(jeu, model, readTimeoutMs, true);
        if (with != null) {
            return with;
        }
        sleepQuiet(120);
        int shortRead = Math.max(2_200, readTimeoutMs * 2 / 3);
        return callGeminiImageApi(jeu, model, shortRead, false);
    }

    private List<String> parseModelList(String csv) {
        List<String> out = new ArrayList<>();
        if (csv == null || csv.isBlank()) {
            return out;
        }
        for (String part : csv.split(",")) {
            String m = part.trim();
            if (!m.isEmpty()) {
                out.add(m);
            }
        }
        return out;
    }

    private boolean shouldUseGeminiCover() {
        return coverGeminiEnabled && geminiApiKey != null && !geminiApiKey.isBlank();
    }

    private String callGeminiImageApi(Jeu jeu, String model, int readTimeoutMs, boolean withImageConfig) {
        int max = Math.max(1, geminiMaxRetries);
        for (int attempt = 1; attempt <= max; attempt++) {
            String raw;
            try {
                raw = exchangeGeminiOnce(jeu, model, readTimeoutMs, withImageConfig);
            } catch (HttpClientErrorException e) {
                if (isRetryableClient(e) && attempt < max) {
                    log.warn("Cover Gemini retry {}/{} (HTTP {})", attempt, max, e.getStatusCode());
                    sleepBackoff(attempt);
                    continue;
                }
                log.warn("Cover Gemini model={} imageConfig={} — HTTP {} — {}",
                        model, withImageConfig, e.getStatusCode(), truncate(e.getResponseBodyAsString(), 500));
                return null;
            } catch (HttpServerErrorException e) {
                if (isRetryableServer(e) && attempt < max) {
                    log.warn("Cover Gemini retry {}/{} (HTTP {})", attempt, max, e.getStatusCode());
                    sleepBackoff(attempt);
                    continue;
                }
                log.warn("Cover Gemini model={} — HTTP {} — {}", model, e.getStatusCode(), truncate(e.getResponseBodyAsString(), 500));
                return null;
            } catch (ResourceAccessException e) {
                if (attempt < max) {
                    log.warn("Cover Gemini retry {}/{} (réseau / timeout: {})", attempt, max, e.getMessage());
                    sleepBackoff(attempt);
                    continue;
                }
                log.warn("Cover Gemini model={} — {}", model, e.getMessage());
                return null;
            } catch (Exception e) {
                log.warn("Cover Gemini model={} — {}", model, e.getMessage());
                return null;
            }

            if (raw == null || raw.isBlank()) {
                return null;
            }

            String image = parseGeminiImageFromResponse(raw);
            if (image != null) {
                return image;
            }

            if (isNonRetryableGeminiErrorJson(raw)) {
                return null;
            }

            if (attempt < max && isResourceExhaustedInJson(raw)) {
                log.warn("Cover Gemini quota / RESOURCE_EXHAUSTED dans le corps, retry {}/{}", attempt, max);
                sleepBackoff(attempt);
                continue;
            }
        }
        return null;
    }

    private String exchangeGeminiOnce(Jeu jeu, String model, int readTimeoutMs, boolean withImageConfig) {
        RestTemplate rt = createGeminiRestTemplate(readTimeoutMs);
        String url = GEMINI_BASE_URL + "/" + model + ":generateContent?key=" + geminiApiKey;
        String prompt = buildGeminiImagePrompt(jeu);

        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseModalities", List.of("TEXT", "IMAGE"));
        if (withImageConfig) {
            Map<String, Object> imageConfig = new LinkedHashMap<>();
            imageConfig.put("aspectRatio", "16:9");
            generationConfig.put("imageConfig", imageConfig);
        }

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("contents", List.of(
                Map.of("parts", List.of(Map.of("text", prompt)))
        ));
        body.put("generationConfig", generationConfig);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = rt.exchange(url, HttpMethod.POST, request, String.class);
        return response.getBody();
    }

    private String parseGeminiImageFromResponse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            if (root.has("error")) {
                log.warn("Cover Gemini erreur API: {}", root.path("error").toString());
                return null;
            }
            JsonNode promptFb = root.path("promptFeedback");
            if (!promptFb.isMissingNode() && promptFb.has("blockReason")) {
                log.warn("Cover Gemini bloqué: {}", promptFb.path("blockReason").asText());
            }
            JsonNode candidates = root.path("candidates");
            if (!candidates.isArray() || candidates.isEmpty()) {
                return null;
            }
            JsonNode parts = candidates.get(0).path("content").path("parts");
            if (!parts.isArray()) {
                return null;
            }
            for (JsonNode part : parts) {
                JsonNode inline = firstNonNull(
                        part.get("inlineData"),
                        part.get("inline_data")
                );
                if (inline == null || inline.isNull()) {
                    continue;
                }
                String mime = mimeFromInline(inline);
                String data = dataFromInline(inline);
                if (data.isBlank()) {
                    continue;
                }
                byte[] decoded;
                try {
                    decoded = Base64.getDecoder().decode(data);
                } catch (IllegalArgumentException e) {
                    log.warn("Cover Gemini: base64 invalide");
                    continue;
                }
                if (decoded.length < MIN_IMAGE_BYTES || decoded.length > MAX_IMAGE_BYTES) {
                    log.warn("Cover Gemini: taille image hors bornes ({})", decoded.length);
                    continue;
                }
                if (sniffImageMime(decoded) == null && (mime == null || !mime.startsWith("image/"))) {
                    continue;
                }
                String safeMime = (mime != null && mime.startsWith("image/")) ? mime : sniffImageMime(decoded);
                if (safeMime == null) {
                    safeMime = MediaType.IMAGE_PNG_VALUE;
                }
                return "data:" + safeMime + ";base64," + Base64.getEncoder().encodeToString(decoded);
            }
        } catch (Exception e) {
            log.warn("Cover Gemini parse: {}", e.getMessage());
        }
        return null;
    }

    private boolean isNonRetryableGeminiErrorJson(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode err = root.path("error");
            if (err.isMissingNode() || err.isNull()) {
                return false;
            }
            int code = err.path("code").asInt(0);
            return code == 400 || code == 401 || code == 403 || code == 404;
        } catch (Exception e) {
            return false;
        }
    }

    private boolean isResourceExhaustedInJson(String raw) {
        try {
            JsonNode root = objectMapper.readTree(raw);
            JsonNode err = root.path("error");
            if (err.isMissingNode() || err.isNull()) {
                return false;
            }
            int code = err.path("code").asInt(0);
            String status = err.path("status").asText("");
            String msg = err.path("message").asText("");
            return code == 429 || code == 503
                    || "RESOURCE_EXHAUSTED".equalsIgnoreCase(status)
                    || msg.toUpperCase().contains("RESOURCE_EXHAUSTED")
                    || msg.toLowerCase().contains("quota");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean isRetryableClient(HttpClientErrorException e) {
        HttpStatus s = HttpStatus.resolve(e.getStatusCode().value());
        if (s == null) {
            return false;
        }
        return s == HttpStatus.TOO_MANY_REQUESTS
                || s == HttpStatus.REQUEST_TIMEOUT;
    }

    private static boolean isRetryableServer(HttpServerErrorException e) {
        HttpStatus s = HttpStatus.resolve(e.getStatusCode().value());
        if (s == null) {
            return false;
        }
        return s == HttpStatus.BAD_GATEWAY
                || s == HttpStatus.SERVICE_UNAVAILABLE
                || s == HttpStatus.GATEWAY_TIMEOUT
                || s == HttpStatus.INTERNAL_SERVER_ERROR;
    }

    private static void sleepBackoff(int attempt) {
        try {
            int ms = 280 * attempt + ThreadLocalRandom.current().nextInt(100, 400);
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void sleepQuiet(int ms) {
        try {
            Thread.sleep(Math.max(0, ms));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static JsonNode firstNonNull(JsonNode... nodes) {
        for (JsonNode n : nodes) {
            if (n != null && !n.isNull() && !n.isMissingNode()) {
                return n;
            }
        }
        return null;
    }

    private static String mimeFromInline(JsonNode inline) {
        if (inline.has("mimeType") && !inline.path("mimeType").asText("").isBlank()) {
            return inline.path("mimeType").asText();
        }
        if (inline.has("mime_type") && !inline.path("mime_type").asText("").isBlank()) {
            return inline.path("mime_type").asText();
        }
        return "image/png";
    }

    private static String dataFromInline(JsonNode inline) {
        if (inline.has("data")) {
            return inline.path("data").asText("");
        }
        return "";
    }

    private static String truncate(String s, int max) {
        if (s == null) return "";
        return s.length() <= max ? s : s.substring(0, max) + "...";
    }

    private String buildGeminiImagePrompt(Jeu jeu) {
        String typeFr = typeJeuLabel(jeu.getTypeJeu());
        String title = jeu.getTitre() != null ? jeu.getTitre().trim() : "";
        String desc = jeu.getDescription() != null ? jeu.getDescription().trim() : "";
        String theme = buildPrompt(jeu);
        var sb = new StringBuilder();
        sb.append("Create ONE high-quality 16:9 landscape cover art for a mobile educational game card (app store / game launcher style). ");
        sb.append("Cinematic or vibrant illustration: detailed, appealing to ages 7–18, energetic colors, depth and lighting, polished commercial game artwork. ");
        sb.append("Category: ").append(typeFr).append(". ");
        if (!title.isEmpty()) {
            sb.append("Visual theme inspired by (do not paint these as readable text): ").append(title).append(". ");
        }
        if (!desc.isEmpty() && desc.length() <= 400) {
            sb.append("Mood and elements: ").append(desc).append(". ");
        }
        sb.append("Keywords: ").append(theme).append(". ");
        sb.append("Strict: no readable text, letters, numbers, logos, or watermarks anywhere on the image.");
        return sb.toString();
    }

    private static String typeJeuLabel(TypeJeu t) {
        if (t == null) return "educational game";
        return switch (t) {
            case QUIZ -> "quiz / trivia";
            case MEMOIRE -> "memory / matching pairs";
            case LOGIQUE -> "logic / puzzles";
            case REFLEXE -> "reflex / speed / action (dynamic motion, energy trails)";
        };
    }

    private String generateViaPollinationsThenSvg(Jeu jeu) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            String remoteUrl = generateCoverUrl(jeu);
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setAccept(List.of(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.parseMediaType("image/webp")));
                headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; StageEduCover/1.0)");
                ResponseEntity<byte[]> response = pollinationsRestTemplate.exchange(
                        remoteUrl,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        byte[].class
                );
                byte[] body = response.getBody();
                MediaType contentType = response.getHeaders().getContentType();
                if (body != null
                        && body.length >= MIN_IMAGE_BYTES
                        && body.length <= MAX_IMAGE_BYTES
                        && isImagePayload(body, contentType)) {
                    String mime = resolveMimeType(contentType, body);
                    return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(body);
                }
                log.warn("Cover Pollinations: réponse ignorée (essai {}, len={}, ct={})",
                        attempt, body == null ? 0 : body.length, contentType);
            } catch (Exception e) {
                log.warn("Cover Pollinations: échec essai {} — {}", attempt, e.getMessage());
            }
            if (attempt < 2) {
                sleepQuiet(400);
            }
        }
        return buildSvgFallbackDataUrl(jeu);
    }

    public String generateCoverUrl(Jeu jeu) {
        return buildPollinationsUrl(buildPrompt(jeu));
    }

    public String generateCoverUrlForPreview(GenerateCoverPreviewRequest request) {
        Jeu draft = Jeu.builder()
                .titre(request.getTitre() != null && !request.getTitre().isBlank() ? request.getTitre() : "Nouveau jeu")
                .description(request.getDescription())
                .typeJeu(request.getTypeJeu() == null ? TypeJeu.QUIZ : request.getTypeJeu())
                .modeJeu(ModeJeu.INDIVIDUEL)
                .build();
        return generateCoverUrl(draft);
    }

    /**
     * Illustration pédagogique pour question IMAGE_WORD (stockée en data URL en base).
     */
    public String generateEducationalIllustrationDataUrl(String subject) {
        String safe = subject == null ? "" : subject.trim();
        if (safe.isBlank()) {
            return null;
        }
        String prompt = "cute colorful educational cartoon illustration of "
                + safe
                + ", simple clean background, child friendly, no text, no watermark";
        return fetchPollinationsAsDataUrl(prompt);
    }

    private String fetchPollinationsAsDataUrl(String prompt) {
        for (int attempt = 1; attempt <= 2; attempt++) {
            String remoteUrl = buildPollinationsUrl(prompt);
            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setAccept(List.of(MediaType.IMAGE_JPEG, MediaType.IMAGE_PNG, MediaType.parseMediaType("image/webp")));
                headers.set(HttpHeaders.USER_AGENT, "Mozilla/5.0 (compatible; StageEduQuestion/1.0)");
                ResponseEntity<byte[]> response = pollinationsRestTemplate.exchange(
                        remoteUrl,
                        HttpMethod.GET,
                        new HttpEntity<>(headers),
                        byte[].class
                );
                byte[] body = response.getBody();
                MediaType contentType = response.getHeaders().getContentType();
                if (body != null
                        && body.length >= MIN_IMAGE_BYTES
                        && body.length <= MAX_IMAGE_BYTES
                        && isImagePayload(body, contentType)) {
                    String mime = resolveMimeType(contentType, body);
                    return "data:" + mime + ";base64," + Base64.getEncoder().encodeToString(body);
                }
            } catch (Exception e) {
                log.warn("Question illustration Pollinations: échec essai {} — {}", attempt, e.getMessage());
            }
            if (attempt < 2) {
                sleepQuiet(400);
            }
        }
        return null;
    }

    public Jeu buildPreviewJeu(GenerateCoverPreviewRequest request) {
        return Jeu.builder()
                .titre(request.getTitre() != null && !request.getTitre().isBlank() ? request.getTitre() : "Nouveau jeu")
                .description(request.getDescription())
                .typeJeu(request.getTypeJeu() == null ? TypeJeu.QUIZ : request.getTypeJeu())
                .modeJeu(ModeJeu.INDIVIDUEL)
                .build();
    }

    private String buildPrompt(Jeu jeu) {
        String type = jeu.getTypeJeu() != null ? jeu.getTypeJeu().name() : "GAME";
        return switch (type) {
            case "QUIZ" -> "vibrant educational quiz game cover illustration, student energy, bright colors, no text";
            case "MEMOIRE" -> "memory card matching game cover, symbols and cards, playful, no text";
            case "LOGIQUE" -> "logic puzzle game cover, geometric shapes mind, no text";
            case "REFLEXE" -> "high speed reflex action game cover, motion blur streaks dynamic character energy anime style no text";
            default -> "colorful educational game cover illustration, no text";
        };
    }

    private String buildPollinationsUrl(String prompt) {
        String encoded = URLEncoder.encode(prompt, StandardCharsets.UTF_8).replace("+", "%20");
        int seed = ThreadLocalRandom.current().nextInt(1, Integer.MAX_VALUE);
        return "https://image.pollinations.ai/prompt/" + encoded
                + "?width=" + POLLINATIONS_WIDTH + "&height=" + POLLINATIONS_HEIGHT
                + "&model=turbo&nologo=true&seed=" + seed;
    }

    private static boolean isImagePayload(byte[] body, MediaType contentType) {
        if (contentType != null && "image".equalsIgnoreCase(contentType.getType())) {
            return true;
        }
        return sniffImageMime(body) != null;
    }

    private static String resolveMimeType(MediaType contentType, byte[] body) {
        if (contentType != null && "image".equalsIgnoreCase(contentType.getType())) {
            return contentType.toString();
        }
        String sniffed = sniffImageMime(body);
        return sniffed != null ? sniffed : MediaType.IMAGE_JPEG_VALUE;
    }

    private static String sniffImageMime(byte[] b) {
        if (b.length >= 3 && b[0] == (byte) 0xFF && b[1] == (byte) 0xD8 && b[2] == (byte) 0xFF) {
            return MediaType.IMAGE_JPEG_VALUE;
        }
        if (b.length >= 8 && b[0] == (byte) 0x89 && b[1] == 0x50 && b[2] == 0x4E && b[3] == 0x47) {
            return MediaType.IMAGE_PNG_VALUE;
        }
        if (b.length >= 12
                && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "image/webp";
        }
        return null;
    }

    private String buildSvgFallbackDataUrl(Jeu jeu) {
        String type = jeu.getTypeJeu() != null ? jeu.getTypeJeu().name() : "GAME";
        String c1 = switch (type) {
            case "QUIZ" -> "#059669";
            case "MEMOIRE" -> "#4f46e5";
            case "LOGIQUE" -> "#d97706";
            case "REFLEXE" -> "#dc2626";
            default -> "#475569";
        };
        String c2 = switch (type) {
            case "QUIZ" -> "#34d399";
            case "MEMOIRE" -> "#818cf8";
            case "LOGIQUE" -> "#fbbf24";
            case "REFLEXE" -> "#f87171";
            default -> "#94a3b8";
        };
        String label = switch (type) {
            case "QUIZ" -> "Quiz";
            case "MEMOIRE" -> "Mémoire";
            case "LOGIQUE" -> "Logique";
            case "REFLEXE" -> "Réflexe";
            default -> "Jeu";
        };
        String safeLabel = escapeXml(label);
        String svg = ""
                + "<svg xmlns=\"http://www.w3.org/2000/svg\" width=\"800\" height=\"450\" viewBox=\"0 0 800 450\">"
                + "<defs><linearGradient id=\"g\" x1=\"0%\" y1=\"0%\" x2=\"100%\" y2=\"100%\">"
                + "<stop offset=\"0%\" stop-color=\"" + c1 + "\"/>"
                + "<stop offset=\"100%\" stop-color=\"" + c2 + "\"/>"
                + "</linearGradient></defs>"
                + "<rect width=\"800\" height=\"450\" fill=\"url(#g)\"/>"
                + "<text x=\"400\" y=\"210\" text-anchor=\"middle\" fill=\"white\" "
                + "font-family=\"system-ui,-apple-system,sans-serif\" font-size=\"32\" font-weight=\"700\">"
                + safeLabel + "</text>"
                + "<text x=\"400\" y=\"255\" text-anchor=\"middle\" fill=\"rgba(255,255,255,0.9)\" "
                + "font-family=\"system-ui,-apple-system,sans-serif\" font-size=\"18\">Cover éducative</text>"
                + "</svg>";
        return "data:image/svg+xml;base64,"
                + Base64.getEncoder().encodeToString(svg.getBytes(StandardCharsets.UTF_8));
    }

    private static String escapeXml(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;");
    }
}
