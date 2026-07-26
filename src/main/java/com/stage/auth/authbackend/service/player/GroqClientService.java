package com.stage.auth.authbackend.service.player;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class GroqClientService {
    private static final String GROQ_API_URL = "https://api.groq.com/openai/v1/chat/completions";

    private final ObjectMapper objectMapper;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${groq.api.key:}")
    private String apiKey;

    @Value("${groq.model:llama-3.1-8b-instant}")
    private String model;

    @Value("${groq.enabled:false}")
    private boolean enabled;

    @Value("${groq.timeout-ms:1500}")
    private int timeoutMs;

    public boolean isReady() {
        return enabled && apiKey != null && !apiKey.isBlank();
    }

    public String requestJson(String systemPrompt, String userPrompt) {
        if (!isReady()) return null;
        try {
            configureTimeouts();
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = Map.of(
                    "model", model,
                    "temperature", 0.1,
                    "response_format", Map.of("type", "json_object"),
                    "messages", List.of(
                            Map.of("role", "system", "content", systemPrompt),
                            Map.of("role", "user", "content", userPrompt)
                    )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(GROQ_API_URL, HttpMethod.POST, request, String.class);
            String raw = response.getBody();
            if (raw == null || raw.isBlank()) return null;
            JsonNode root = objectMapper.readTree(raw);
            String text = root.path("choices").path(0).path("message").path("content").asText("");
            return text == null || text.isBlank() ? null : text.trim();
        } catch (Exception e) {
            log.warn("Groq request failed, fallback to deterministic flow: {}", e.getMessage());
            return null;
        }
    }

    private void configureTimeouts() {
        if (restTemplate.getRequestFactory() instanceof org.springframework.http.client.SimpleClientHttpRequestFactory factory) {
            factory.setConnectTimeout(timeoutMs);
            factory.setReadTimeout(timeoutMs);
            return;
        }
        org.springframework.http.client.SimpleClientHttpRequestFactory factory = new org.springframework.http.client.SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(timeoutMs);
        factory.setReadTimeout(timeoutMs);
        restTemplate.setRequestFactory(factory);
    }
}
