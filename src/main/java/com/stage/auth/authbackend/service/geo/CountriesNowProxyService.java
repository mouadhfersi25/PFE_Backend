package com.stage.auth.authbackend.service.geo;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.*;

/**
 * Proxy vers l'API Countries Now (https://countriesnow.space)
 * Évite les problèmes CORS en appelant depuis le backend.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CountriesNowProxyService {

    private static final String BASE_URL = "https://countriesnow.space/api/v0.1";
    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public List<Map<String, String>> getCountries() {
        try {
            String url = BASE_URL + "/countries";
            String json = restTemplate.getForObject(url, String.class);
            if (json == null) return List.of();

            JsonNode root = objectMapper.readTree(json);
            if (root.get("error") != null && root.get("error").asBoolean()) return List.of();

            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) return List.of();

            List<Map<String, String>> result = new ArrayList<>();
            for (JsonNode c : data) {
                String name = getText(c, "name", "country");
                if (name != null && !name.isBlank()) {
                    result.add(Map.of("name", name));
                }
            }
            result.sort(Comparator.comparing(m -> m.get("name")));
            return result;
        } catch (Exception e) {
            log.warn("Countries Now API getCountries failed: {}", e.getMessage());
            return List.of();
        }
    }

    public List<String> getStates(String countryName) {
        try {
            // GET /countries/states retourne TOUS les pays avec leurs états
            // Format: { data: [ { name, states: [ { name } ] } ] }
            String url = BASE_URL + "/countries/states";
            String json = restTemplate.getForObject(url, String.class);
            if (json == null) return List.of();

            JsonNode root = objectMapper.readTree(json);
            if (root.get("error") != null && root.get("error").asBoolean()) return List.of();

            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) return List.of();

            String searchName = countryName.strip();
            for (JsonNode country : data) {
                String cName = getText(country, "name", "country");
                if (cName != null && cName.equalsIgnoreCase(searchName)) {
                    JsonNode states = country.get("states");
                    if (states == null || !states.isArray()) return List.of();
                    List<String> result = new ArrayList<>();
                    for (JsonNode s : states) {
                        String state = getText(s, "name", "state");
                        if (state != null && !state.isBlank()) result.add(state);
                    }
                    result.sort(String::compareTo);
                    return result;
                }
            }
            return List.of();
        } catch (Exception e) {
            log.warn("Countries Now API getStates failed for {}: {}", countryName, e.getMessage());
            return List.of();
        }
    }

    private String getText(JsonNode node, String... keys) {
        for (String k : keys) {
            JsonNode n = node.get(k);
            if (n != null && n.isTextual()) return n.asText();
        }
        return null;
    }
}
