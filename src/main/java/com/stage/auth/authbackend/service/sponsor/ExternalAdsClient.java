package com.stage.auth.authbackend.service.sponsor;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class ExternalAdsClient {

    private final ObjectMapper objectMapper;

    @Value("${external.ads.enabled:false}")
    private boolean enabled;

    @Value("${external.ads.base-url:}")
    private String baseUrl;

    @Value("${external.ads.api-key:}")
    private String apiKey;

    private RestClient restClient() {
        return RestClient.builder()
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .defaultHeader("X-API-Key", apiKey == null ? "" : apiKey)
                .build();
    }

    public boolean isEnabled() {
        return enabled && baseUrl != null && !baseUrl.isBlank();
    }

    public JsonNode get(String path) {
        if (!isEnabled()) return null;
        try {
            String body = restClient().get()
                    .uri(path)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) return null;
            return objectMapper.readTree(body);
        } catch (Exception e) {
            return null;
        }
    }

    public JsonNode get(String path, Map<String, Object> queryParams) {
        if (!isEnabled()) return null;
        try {
            String body = restClient().get()
                    .uri(uriBuilder -> {
                        var b = uriBuilder.path(path);
                        if (queryParams != null) {
                            queryParams.forEach((k, v) -> {
                                if (v != null) b.queryParam(k, v);
                            });
                        }
                        return b.build();
                    })
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) return null;
            return objectMapper.readTree(body);
        } catch (Exception e) {
            return null;
        }
    }

    public JsonNode post(String path, Object payload) {
        if (!isEnabled()) return null;
        try {
            String body = restClient().post()
                    .uri(path)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) return null;
            return objectMapper.readTree(body);
        } catch (Exception e) {
            return null;
        }
    }

    public JsonNode put(String path, Object payload) {
        if (!isEnabled()) return null;
        try {
            String body = restClient().put()
                    .uri(path)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) return null;
            return objectMapper.readTree(body);
        } catch (Exception e) {
            return null;
        }
    }

    public JsonNode patch(String path, Object payload) {
        if (!isEnabled()) return null;
        try {
            String body = restClient().patch()
                    .uri(path)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
            if (body == null || body.isBlank()) return null;
            return objectMapper.readTree(body);
        } catch (Exception e) {
            return null;
        }
    }

    public boolean delete(String path) {
        if (!isEnabled()) return false;
        try {
            restClient().delete()
                    .uri(path)
                    .retrieve()
                    .toBodilessEntity();
            return true;
        } catch (Exception e) {
            return false;
        }
    }
}
