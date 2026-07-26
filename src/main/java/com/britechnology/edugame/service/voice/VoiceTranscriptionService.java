package com.britechnology.edugame.service.voice;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.britechnology.edugame.exception.ApiException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class VoiceTranscriptionService {

    private static final String GROQ_TRANSCRIBE_URL = "https://api.groq.com/openai/v1/audio/transcriptions";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${groq.api.key:}")
    private String groqApiKey;

    @Value("${groq.enabled:false}")
    private boolean groqEnabled;

    @Value("${voice.transcription.timeout-ms:15000}")
    private int timeoutMs;

    @Value("${voice.transcription.browser-fallback-enabled:true}")
    private boolean browserFallbackEnabled;

    public String transcribe(MultipartFile audio, String langue, String browserTranscript) {
        if (audio != null && !audio.isEmpty() && isGroqReady()) {
            try {
                return transcribeWithGroq(audio, langue);
            } catch (Exception ex) {
                log.warn("Groq transcription failed: {}", ex.getMessage());
            }
        }

        if (browserFallbackEnabled && browserTranscript != null && !browserTranscript.isBlank()) {
            return browserTranscript.trim();
        }

        throw ApiException.badRequest(
                "Transcription vocale indisponible. Configurez GROQ_API_KEY ou utilisez la reconnaissance navigateur."
        );
    }

    private boolean isGroqReady() {
        return groqEnabled && groqApiKey != null && !groqApiKey.isBlank();
    }

    private String transcribeWithGroq(MultipartFile audio, String langue) throws Exception {
        if (audio == null || audio.isEmpty()) {
            throw ApiException.badRequest("Fichier audio requis");
        }
        configureTimeouts();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);
        headers.setBearerAuth(groqApiKey);

        ByteArrayResource resource = new ByteArrayResource(audio.getBytes()) {
            @Override
            public String getFilename() {
                String name = audio.getOriginalFilename();
                return name != null && !name.isBlank() ? name : "recording.wav";
            }
        };

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("file", resource);
        body.add("model", "whisper-large-v3");
        body.add("response_format", "json");
        if (langue != null && !langue.isBlank()) {
            body.add("language", langue.trim().toLowerCase());
        }

        HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);
        ResponseEntity<String> response = restTemplate.exchange(
                GROQ_TRANSCRIBE_URL,
                HttpMethod.POST,
                request,
                String.class
        );

        String raw = response.getBody();
        if (raw == null || raw.isBlank()) {
            throw ApiException.badRequest("Transcription vide");
        }
        JsonNode root = objectMapper.readTree(raw);
        String text = root.path("text").asText("").trim();
        if (text.isEmpty()) {
            throw ApiException.badRequest("Aucune parole détectée dans l'enregistrement");
        }
        return text;
    }

    private void configureTimeouts() {
        if (restTemplate.getRequestFactory() instanceof org.springframework.http.client.SimpleClientHttpRequestFactory factory) {
            factory.setConnectTimeout(timeoutMs);
            factory.setReadTimeout(timeoutMs);
            return;
        }
        org.springframework.http.client.SimpleClientHttpRequestFactory factory =
                new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        restTemplate.setRequestFactory(factory);
    }
}
