package com.stage.auth.authbackend.service.educator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stage.auth.authbackend.dto.educator.ReflexSettingsDTO;
import com.stage.auth.authbackend.entity.Jeu;
import com.stage.auth.authbackend.entity.ReflexModel;
import com.stage.auth.authbackend.entity.ReflexStimulusType;
import com.stage.auth.authbackend.entity.TypeJeu;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.game.JeuRepository;
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
public class AiReflexGenerationService {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    private final ObjectMapper objectMapper;
    private final JeuRepository jeuRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.gemini.api-key:}")
    private String apiKey;

    @Value("${ai.gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${ai.gemini.enabled:false}")
    private boolean enabled;

    public ReflexSettingsDTO generatePreview(Long gameId) {
        if (!enabled) {
            throw ApiException.badRequest("Le module IA Gemini est désactivé (ai.gemini.enabled=false)");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw ApiException.badRequest("Clé API Gemini absente. Vérifiez GEMINI_API_KEY.");
        }
        if (gameId == null) {
            throw ApiException.badRequest("gameId est requis");
        }

        Jeu jeu = jeuRepository.findById(gameId)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        if (jeu.getTypeJeu() != TypeJeu.REFLEXE) {
            throw ApiException.badRequest("La génération IA est disponible uniquement pour les jeux de type REFLEXE");
        }
        EducatorGameEditPolicy.requireDraft(jeu);

        String prompt = buildPrompt(jeu);
        String responseText = callGemini(prompt);
        try {
            JsonNode root = objectMapper.readTree(extractJson(responseText));
            ReflexModel modeleReflexe = normalizeModel(root.path("modeleReflexe").asText(null));
            int nombreRounds = clamp(root.path("nombreRounds").asInt(10), 1, 30);
            int tempsReactionMaxMs = clamp(root.path("tempsReactionMaxMs").asInt(2000), 500, 5000);
            ReflexStimulusType typeStimuli = normalizeStimuli(root.path("typeStimuli").asText(null));
            int difficulte = clamp(root.path("difficulte").asInt(jeu.getDifficulte() == null ? 5 : jeu.getDifficulte()), 0, 10);
            int noGoRatio = clamp(root.path("noGoRatio").asInt(30), 10, 90);
            int choiceTargetCount = clamp(root.path("choiceTargetCount").asInt(3), 2, 6);

            return ReflexSettingsDTO.builder()
                    .jeuId(jeu.getId())
                    .jeuTitre(jeu.getTitre())
                    .modeleReflexe(modeleReflexe)
                    .nombreRounds(nombreRounds)
                    .tempsReactionMaxMs(tempsReactionMaxMs)
                    .typeStimuli(typeStimuli)
                    .difficulte(difficulte)
                    .noGoRatio(noGoRatio)
                    .choiceTargetCount(choiceTargetCount)
                    .build();
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini reflex preview parse failed for game {}: {}", gameId, e.getMessage());
            throw ApiException.internalServerError("Impossible d'interpréter la configuration IA");
        }
    }

    private String buildPrompt(Jeu jeu) {
        String ageRange = (jeu.getAgeMin() != null || jeu.getAgeMax() != null)
                ? (String.valueOf(jeu.getAgeMin() == null ? "?" : jeu.getAgeMin()) + "-" + (jeu.getAgeMax() == null ? "?" : jeu.getAgeMax()))
                : "non précisée";
        String duration = jeu.getDureeMinutes() == null ? "non précisée" : jeu.getDureeMinutes() + " minutes";
        String description = (jeu.getDescription() == null || jeu.getDescription().isBlank()) ? "Aucune description fournie." : jeu.getDescription().trim();
        int diff = clamp(jeu.getDifficulte() == null ? 5 : jeu.getDifficulte(), 0, 10);

        return """
                Tu es expert en game design éducatif. Génère une configuration de jeu de réflexe en français.

                CONTEXTE JEU:
                - Titre: %s
                - Description: %s
                - Difficulté (0-10): %d
                - Tranche d'âge: %s
                - Durée visée: %s

                RÈGLES:
                - Choisir un modèle parmi: CLASSIC, GO_NO_GO, CHOICE_REACTION
                - Choisir nombreRounds entre 1 et 30
                - Choisir tempsReactionMaxMs entre 500 et 5000
                - Choisir typeStimuli parmi: TARGET_ICON, COLOR_FLASH, MIXED
                - Choisir difficulte entre 0 et 10
                - Si modèle GO_NO_GO: noGoRatio entre 10 et 90
                - Si modèle CHOICE_REACTION: choiceTargetCount entre 2 et 6

                FORMAT OBLIGATOIRE:
                Retourne UNIQUEMENT un JSON valide:
                {
                  "modeleReflexe": "CLASSIC",
                  "nombreRounds": 10,
                  "tempsReactionMaxMs": 2000,
                  "typeStimuli": "TARGET_ICON",
                  "difficulte": 5,
                  "noGoRatio": 30,
                  "choiceTargetCount": 3
                }
                """.formatted(jeu.getTitre(), description, diff, ageRange, duration);
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
            log.warn("Gemini reflex call failed: {}", e.getMessage());
            throw ApiException.internalServerError("Échec appel Gemini: " + e.getMessage());
        }
    }

    private static ReflexModel normalizeModel(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase();
        if ("GO_NO_GO".equals(value)) {
            return ReflexModel.GO_NO_GO;
        }
        if ("CHOICE_REACTION".equals(value)) {
            return ReflexModel.CHOICE_REACTION;
        }
        return ReflexModel.CLASSIC;
    }

    private static ReflexStimulusType normalizeStimuli(String raw) {
        String value = raw == null ? "" : raw.trim().toUpperCase();
        if ("COLOR_FLASH".equals(value)) {
            return ReflexStimulusType.COLOR_FLASH;
        }
        if ("MIXED".equals(value)) {
            return ReflexStimulusType.MIXED;
        }
        return ReflexStimulusType.TARGET_ICON;
    }

    private static int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
}
