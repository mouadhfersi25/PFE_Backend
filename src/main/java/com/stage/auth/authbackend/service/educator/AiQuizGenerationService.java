package com.stage.auth.authbackend.service.educator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stage.auth.authbackend.dto.educator.QuizQuestionDTO;
import com.stage.auth.authbackend.entity.Jeu;
import com.stage.auth.authbackend.entity.TypeJeu;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.game.JeuRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiQuizGenerationService {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    private final ObjectMapper objectMapper;
    private final JeuRepository jeuRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.gemini.api-key:}")
    private String apiKey;

    @Value("${ai.gemini.model:gemini-1.5-flash}")
    private String model;

    @Value("${ai.gemini.enabled:false}")
    private boolean enabled;

    public List<QuizQuestionDTO> generatePreview(Long gameId, Integer requestedCount) {
        if (!enabled) {
            throw ApiException.badRequest("Le module IA Gemini est désactivé (ai.gemini.enabled=false)");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw ApiException.badRequest("Clé API Gemini absente. Vérifiez GEMINI_API_KEY.");
        }
        if (gameId == null) {
            throw ApiException.badRequest("gameId est requis");
        }

        final int count = normalizeCount(requestedCount);
        Jeu jeu = jeuRepository.findById(gameId)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));

        if (jeu.getTypeJeu() != TypeJeu.QUIZ) {
            throw ApiException.badRequest("La génération IA est disponible uniquement pour les jeux de type QUIZ");
        }
        EducatorGameEditPolicy.requireDraft(jeu);

        String prompt = buildPrompt(jeu, count);
        String responseText = callGemini(prompt);

        try {
            JsonNode root = objectMapper.readTree(extractJson(responseText));
            JsonNode questionsNode = root.path("questions");
            if (!questionsNode.isArray() || questionsNode.isEmpty()) {
                throw ApiException.internalServerError("La réponse IA ne contient pas de questions exploitables");
            }

            List<QuizQuestionDTO> generated = new ArrayList<>();
            for (JsonNode q : questionsNode) {
                String contenu = cleanText(q.path("contenu").asText(null));
                String bonneReponse = cleanText(q.path("bonneReponse").asText(null));
                String explication = cleanText(q.path("explication").asText(null));
                Integer difficulte = normalizeDifficulty(q.path("difficulte").asInt(jeu.getDifficulte() == null ? 5 : jeu.getDifficulte()));

                List<String> options = new ArrayList<>();
                if (q.path("options").isArray()) {
                    for (JsonNode opt : q.path("options")) {
                        String cleanedOpt = cleanText(opt.asText(null));
                        if (cleanedOpt != null && !cleanedOpt.isBlank() && !options.contains(cleanedOpt)) {
                            options.add(cleanedOpt);
                        }
                    }
                }

                if (contenu == null || contenu.isBlank() || bonneReponse == null || bonneReponse.isBlank()) {
                    continue;
                }
                if (!options.isEmpty() && options.stream().noneMatch(o -> o.equalsIgnoreCase(bonneReponse))) {
                    options.add(bonneReponse);
                }
                if (!options.isEmpty() && options.size() < 2) {
                    continue;
                }

                generated.add(QuizQuestionDTO.builder()
                        .jeuId(jeu.getId())
                        .jeuTitre(jeu.getTitre())
                        .contenu(contenu)
                        .bonneReponse(bonneReponse)
                        .options(options.isEmpty() ? null : options)
                        .explication(explication)
                        .difficulte(difficulte)
                        .build());
            }

            if (generated.isEmpty()) {
                throw ApiException.internalServerError("Aucune question valide n'a été générée par l'IA");
            }
            return generated.stream().limit(count).collect(Collectors.toList());
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini preview parse failed for game {}: {}", gameId, e.getMessage());
            throw ApiException.internalServerError("Impossible d'interpréter la réponse IA");
        }
    }

    public Map<String, Object> ping() {
        if (!enabled) {
            throw ApiException.badRequest("Le module IA Gemini est désactivé (ai.gemini.enabled=false)");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw ApiException.badRequest("Clé API Gemini absente. Vérifiez GEMINI_API_KEY.");
        }

        try {
            String url = GEMINI_BASE_URL + "/" + model + ":generateContent?key=" + apiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", "Réponds exactement: PONG")
                                    )
                            )
                    )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            String raw = response.getBody();

            if (raw == null || raw.isBlank()) {
                throw ApiException.internalServerError("Réponse vide depuis Gemini");
            }

            JsonNode root = objectMapper.readTree(raw);
            String text = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("");

            if (text.isBlank()) {
                throw ApiException.internalServerError("Réponse Gemini invalide ou non lisible");
            }

            return Map.of(
                    "message", "Connexion Gemini OK",
                    "model", model,
                    "reply", text.trim()
            );
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini ping failed: {}", e.getMessage());
            throw ApiException.internalServerError("Échec appel Gemini: " + e.getMessage());
        }
    }

    private String callGemini(String prompt) {
        try {
            String url = GEMINI_BASE_URL + "/" + model + ":generateContent?key=" + apiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            Map<String, Object> body = Map.of(
                    "contents", List.of(
                            Map.of(
                                    "parts", List.of(
                                            Map.of("text", prompt)
                                    )
                            )
                    )
            );

            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            String raw = response.getBody();

            if (raw == null || raw.isBlank()) {
                throw ApiException.internalServerError("Réponse vide depuis Gemini");
            }

            JsonNode root = objectMapper.readTree(raw);
            String text = root.path("candidates")
                    .path(0)
                    .path("content")
                    .path("parts")
                    .path(0)
                    .path("text")
                    .asText("");

            if (text.isBlank()) {
                throw ApiException.internalServerError("Réponse Gemini invalide ou non lisible");
            }
            return text;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini call failed: {}", e.getMessage());
            throw ApiException.internalServerError("Échec appel Gemini: " + e.getMessage());
        }
    }

    private int normalizeCount(Integer requestedCount) {
        if (requestedCount == null) return 5;
        if (requestedCount < 1) return 1;
        return Math.min(requestedCount, 10);
    }

    private int normalizeDifficulty(Integer difficulty) {
        if (difficulty == null) return 5;
        if (difficulty < 0) return 0;
        return Math.min(difficulty, 10);
    }

    private String buildPrompt(Jeu jeu, int count) {
        Integer diff = normalizeDifficulty(jeu.getDifficulte());
        String ageRange = (jeu.getAgeMin() != null || jeu.getAgeMax() != null)
                ? (String.valueOf(jeu.getAgeMin() == null ? "?" : jeu.getAgeMin()) + "-" + (jeu.getAgeMax() == null ? "?" : jeu.getAgeMax()))
                : "non précisée";
        String duration = jeu.getDureeMinutes() == null ? "non précisée" : jeu.getDureeMinutes() + " minutes";
        String description = (jeu.getDescription() == null || jeu.getDescription().isBlank()) ? "Aucune description fournie." : jeu.getDescription().trim();

        return """
                Tu es un expert en pédagogie. Génère des questions de quiz en français pour enfants/ados selon les contraintes.

                CONTRAINTES:
                - Titre du quiz: %s
                - Description: %s
                - Difficulté (0-10): %d
                - Tranche d'âge: %s
                - Durée visée: %s
                - Nombre de questions: %d
                - Questions variées, claires, non ambiguës.
                - Réponses adaptées au niveau d'âge.
                - Éviter toute question dangereuse, violente, ou inadaptée.

                FORMAT OBLIGATOIRE:
                Retourne UNIQUEMENT un JSON valide (sans texte avant/après), au format:
                {
                  "questions": [
                    {
                      "contenu": "Question ...",
                      "options": ["Option A", "Option B", "Option C", "Option D"],
                      "bonneReponse": "Option A",
                      "explication": "Courte explication pédagogique",
                      "difficulte": 0
                    }
                  ]
                }
                """.formatted(jeu.getTitre(), description, diff, ageRange, duration, count);
    }

    private String extractJson(String rawText) {
        String text = rawText == null ? "" : rawText.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                text = text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return text.substring(start, end + 1);
        }
        return text;
    }

    private String cleanText(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
