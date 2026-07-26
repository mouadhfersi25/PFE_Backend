package com.stage.auth.authbackend.service.admin;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stage.auth.authbackend.dto.game.GameAiReviewDTO;
import com.stage.auth.authbackend.entity.CarteMemoire;
import com.stage.auth.authbackend.entity.EtatJeu;
import com.stage.auth.authbackend.entity.Jeu;
import com.stage.auth.authbackend.entity.ParametresReflexe;
import com.stage.auth.authbackend.entity.PuzzleLogique;
import com.stage.auth.authbackend.entity.Question;
import com.stage.auth.authbackend.entity.TypeJeu;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.game.CarteMemoireRepository;
import com.stage.auth.authbackend.repository.game.JeuRepository;
import com.stage.auth.authbackend.repository.game.ParametresReflexeRepository;
import com.stage.auth.authbackend.repository.game.PuzzleLogiqueRepository;
import com.stage.auth.authbackend.repository.game.QuestionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiGameReviewService {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    private final ObjectMapper objectMapper;
    private final JeuRepository jeuRepository;
    private final QuestionRepository questionRepository;
    private final CarteMemoireRepository carteMemoireRepository;
    private final PuzzleLogiqueRepository puzzleLogiqueRepository;
    private final ParametresReflexeRepository parametresReflexeRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.gemini.api-key:}")
    private String apiKey;

    @Value("${ai.gemini.model:gemini-2.5-flash}")
    private String model;

    @Value("${ai.gemini.enabled:false}")
    private boolean enabled;

    public GameAiReviewDTO reviewGame(Long gameId) {
        if (gameId == null) throw ApiException.badRequest("gameId est requis");

        Jeu jeu = jeuRepository.findById(gameId).orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        if (jeu.getEtat() != EtatJeu.EN_ATTENTE) {
            throw ApiException.badRequest("L'analyse IA est autorisée uniquement pour les jeux finalisés en attente de validation admin.");
        }

        // Fallback déterministe: si l'IA externe est désactivée/non configurée,
        // on renvoie une analyse locale afin de ne jamais bloquer l'admin.
        if (!enabled || apiKey == null || apiKey.isBlank()) {
            return buildFallbackReview(jeu, "fallback-local");
        }

        String prompt = buildReviewPrompt(jeu);
        String rawText;
        try {
            rawText = callGemini(prompt);
        } catch (Exception e) {
            log.warn("Gemini unavailable for game {} => fallback local review. reason={}", gameId, e.getMessage());
            return buildFallbackReview(jeu, "fallback-local");
        }

        try {
            JsonNode root = objectMapper.readTree(extractJson(rawText));
            int score = clamp(root.path("score").asInt(50), 0, 100);
            String riskLevel = normalizeRisk(root.path("riskLevel").asText("MEDIUM"));
            String suggestedAction = normalizeAction(root.path("suggestedAction").asText("REVIEW_REQUIRED"));
            String summary = clean(root.path("summary").asText("Analyse IA disponible."));
            List<String> strengths = jsonArrayToList(root.path("strengths"));
            List<String> issues = jsonArrayToList(root.path("issues"));
            List<String> recommendations = jsonArrayToList(root.path("recommendations"));

            return GameAiReviewDTO.builder()
                    .gameId(jeu.getId())
                    .gameTitle(jeu.getTitre())
                    .model(model)
                    .score(score)
                    .riskLevel(riskLevel)
                    .suggestedAction(suggestedAction)
                    .summary(summary)
                    .strengths(strengths)
                    .issues(issues)
                    .recommendations(recommendations)
                    .build();
        } catch (Exception e) {
            log.warn("AI game review parse failed for game {}: {}", gameId, e.getMessage());
            return buildFallbackReview(jeu, "fallback-local");
        }
    }

    private String buildReviewPrompt(Jeu jeu) {
        String meta = """
                Métadonnées jeu:
                - Titre: %s
                - Description: %s
                - Type: %s
                - Mode: %s
                - Difficulté: %s
                - Age min/max: %s / %s
                - Durée (minutes): %s
                - Etat actuel: %s
                """.formatted(
                safe(jeu.getTitre()),
                safe(jeu.getDescription()),
                safe(jeu.getTypeJeu() != null ? jeu.getTypeJeu().name() : null),
                safe(jeu.getModeJeu() != null ? jeu.getModeJeu().name() : null),
                safe(jeu.getDifficulte() != null ? String.valueOf(jeu.getDifficulte()) : null),
                safe(jeu.getAgeMin() != null ? String.valueOf(jeu.getAgeMin()) : null),
                safe(jeu.getAgeMax() != null ? String.valueOf(jeu.getAgeMax()) : null),
                safe(jeu.getDureeMinutes() != null ? String.valueOf(jeu.getDureeMinutes()) : null),
                safe(jeu.getEtat() != null ? jeu.getEtat().name() : null)
        );

        String content;
        if (jeu.getTypeJeu() == TypeJeu.QUIZ) {
            List<Question> questions = questionRepository.findByJeuId(jeu.getId());
            StringBuilder sb = new StringBuilder("Contenu QUIZ:\n");
            if (questions.isEmpty()) {
                sb.append("- Aucune question ajoutée\n");
            } else {
                int i = 1;
                for (Question q : questions) {
                    sb.append("- Q").append(i++).append(": ").append(safe(q.getContenu()))
                            .append(" | bonneReponse=").append(safe(q.getBonneReponse()))
                            .append(" | difficulte=").append(safe(q.getDifficulte() != null ? q.getDifficulte().toString() : null))
                            .append("\n");
                }
            }
            content = sb.toString();
        } else if (jeu.getTypeJeu() == TypeJeu.MEMOIRE) {
            List<CarteMemoire> cards = carteMemoireRepository.findByJeuId(jeu.getId());
            StringBuilder sb = new StringBuilder("Contenu MEMOIRE:\n");
            if (cards.isEmpty()) {
                sb.append("- Aucune carte ajoutée\n");
            } else {
                int i = 1;
                for (CarteMemoire c : cards) {
                    sb.append("- C").append(i++).append(": symbole=").append(safe(c.getSymbole()))
                            .append(" | pairKey=").append(safe(c.getPairKey()))
                            .append(" | categorie=").append(safe(c.getCategorie()))
                            .append("\n");
                }
            }
            content = sb.toString();
        } else {
            content = "Contenu détaillé non extrait pour ce type de jeu. Évaluer selon les métadonnées disponibles.";
        }

        return """
                Tu es un assistant de modération pédagogique. Analyse ce jeu éducatif et retourne UNIQUEMENT un JSON valide.
                Objectif: aider l'admin à décider si le jeu peut être accepté ou nécessite des corrections.

                Critères à évaluer:
                1) Cohérence métadonnées <-> contenu
                2) Adéquation difficulté / tranche d'âge / durée
                3) Qualité pédagogique (clarté, absence d'ambiguïté)
                4) Sécurité / contenu inadapté
                5) Complétude du contenu

                %s

                %s

                FORMAT JSON OBLIGATOIRE:
                {
                  "score": 0,
                  "riskLevel": "LOW|MEDIUM|HIGH",
                  "suggestedAction": "REVIEW_OK|REVIEW_REQUIRED|HIGH_RISK",
                  "summary": "Résumé clair pour admin",
                  "strengths": ["point fort 1", "point fort 2"],
                  "issues": ["problème 1", "problème 2"],
                  "recommendations": ["action 1", "action 2"]
                }
                """.formatted(meta, content);
    }

    private String callGemini(String prompt) {
        try {
            String url = GEMINI_BASE_URL + "/" + model + ":generateContent?key=" + apiKey;
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            Map<String, Object> body = Map.of(
                    "contents", List.of(Map.of("parts", List.of(Map.of("text", prompt))))
            );
            HttpEntity<Map<String, Object>> request = new HttpEntity<>(body, headers);
            ResponseEntity<String> response = restTemplate.exchange(url, HttpMethod.POST, request, String.class);
            String raw = response.getBody();
            if (raw == null || raw.isBlank()) throw ApiException.internalServerError("Réponse vide Gemini");

            JsonNode root = objectMapper.readTree(raw);
            String text = root.path("candidates").path(0).path("content").path("parts").path(0).path("text").asText("");
            if (text.isBlank()) throw ApiException.internalServerError("Réponse Gemini non lisible");
            return text;
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            log.warn("Gemini review call failed: {}", e.getMessage());
            throw ApiException.internalServerError("Échec appel Gemini: " + e.getMessage());
        }
    }

    private List<String> jsonArrayToList(JsonNode node) {
        List<String> out = new ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode item : node) {
                String value = clean(item.asText(""));
                if (value != null && !value.isBlank()) out.add(value);
            }
        }
        return out;
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
    }

    private String normalizeRisk(String risk) {
        String r = safe(risk).toUpperCase();
        if (!r.equals("LOW") && !r.equals("MEDIUM") && !r.equals("HIGH")) return "MEDIUM";
        return r;
    }

    private String normalizeAction(String action) {
        String a = safe(action).toUpperCase();
        if (!a.equals("REVIEW_OK") && !a.equals("REVIEW_REQUIRED") && !a.equals("HIGH_RISK")) return "REVIEW_REQUIRED";
        return a;
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
        if (start >= 0 && end > start) return text.substring(start, end + 1);
        return text;
    }

    private String safe(String value) {
        return value == null || value.isBlank() ? "non renseigné" : value.trim();
    }

    private String clean(String value) {
        if (value == null) return null;
        String v = value.trim();
        return v.isBlank() ? null : v;
    }

    private GameAiReviewDTO buildFallbackReview(Jeu jeu, String usedModel) {
        int score = 75;
        List<String> strengths = new ArrayList<>();
        List<String> issues = new ArrayList<>();
        List<String> recommendations = new ArrayList<>();

        if (jeu.getDescription() != null && !jeu.getDescription().isBlank()) {
            strengths.add("Description du jeu renseignée.");
        } else {
            issues.add("Description manquante.");
            recommendations.add("Ajouter une description claire des objectifs pédagogiques.");
            score -= 10;
        }

        if (jeu.getDifficulte() == null) {
            issues.add("Difficulté non renseignée.");
            recommendations.add("Renseigner le niveau de difficulté attendu.");
            score -= 8;
        }

        if (jeu.getAgeMin() == null || jeu.getAgeMax() == null) {
            issues.add("Tranche d'âge incomplète.");
            recommendations.add("Définir une tranche d'âge cohérente.");
            score -= 8;
        } else if (jeu.getAgeMin() > jeu.getAgeMax()) {
            issues.add("Tranche d'âge incohérente (min > max).");
            recommendations.add("Corriger l'âge minimum et maximum.");
            score -= 12;
        } else {
            strengths.add("Tranche d'âge définie.");
        }

        if (jeu.getDureeMinutes() == null || jeu.getDureeMinutes() <= 0) {
            issues.add("Durée estimée absente ou invalide.");
            recommendations.add("Renseigner une durée estimée réaliste.");
            score -= 8;
        } else {
            strengths.add("Durée estimée renseignée.");
        }

        switch (jeu.getTypeJeu()) {
            case QUIZ -> {
                List<Question> questions = questionRepository.findByJeuId(jeu.getId());
                if (questions.isEmpty()) {
                    issues.add("Aucune question quiz configurée.");
                    recommendations.add("Ajouter des questions avec bonnes réponses et difficultés.");
                    score -= 20;
                } else {
                    strengths.add("Quiz avec " + questions.size() + " question(s).");
                }
            }
            case MEMOIRE -> {
                List<CarteMemoire> cards = carteMemoireRepository.findByJeuId(jeu.getId());
                if (cards.size() < 2) {
                    issues.add("Contenu mémoire insuffisant.");
                    recommendations.add("Ajouter des paires de cartes en quantité suffisante.");
                    score -= 20;
                } else {
                    strengths.add("Jeu mémoire avec " + cards.size() + " carte(s).");
                }
            }
            case LOGIQUE -> {
                List<PuzzleLogique> puzzles = puzzleLogiqueRepository.findByJeuId(jeu.getId());
                if (puzzles.isEmpty()) {
                    issues.add("Aucun puzzle logique configuré.");
                    recommendations.add("Ajouter au moins un puzzle avec réponse attendue.");
                    score -= 20;
                } else {
                    strengths.add("Jeu logique avec " + puzzles.size() + " puzzle(s).");
                }
            }
            case REFLEXE -> {
                ParametresReflexe settings = parametresReflexeRepository.findByJeuId(jeu.getId()).orElse(null);
                if (settings == null) {
                    issues.add("Paramètres réflexe non configurés.");
                    recommendations.add("Configurer les paramètres réflexe (rounds, stimuli, timing).");
                    score -= 20;
                } else {
                    strengths.add("Paramètres réflexe configurés.");
                }
            }
            default -> {
                issues.add("Type de jeu non reconnu pour une validation détaillée.");
                recommendations.add("Vérifier la configuration du type de jeu.");
                score -= 10;
            }
        }

        score = clamp(score, 0, 100);
        String riskLevel = score >= 80 ? "LOW" : score >= 60 ? "MEDIUM" : "HIGH";
        String suggestedAction = score >= 80 ? "REVIEW_OK" : score >= 60 ? "REVIEW_REQUIRED" : "HIGH_RISK";
        String summary = score >= 80
                ? "Analyse locale: le jeu est globalement cohérent pour une validation admin."
                : score >= 60
                ? "Analyse locale: le jeu est exploitable mais nécessite des ajustements avant validation."
                : "Analyse locale: plusieurs points critiques doivent être corrigés avant validation.";

        if (strengths.isEmpty()) strengths.add("Structure de jeu détectée.");
        if (issues.isEmpty()) issues.add("Aucun problème critique détecté par l'analyse locale.");
        if (recommendations.isEmpty()) recommendations.add("Effectuer une revue pédagogique rapide avant décision finale.");

        return GameAiReviewDTO.builder()
                .gameId(jeu.getId())
                .gameTitle(jeu.getTitre())
                .model(usedModel)
                .score(score)
                .riskLevel(riskLevel)
                .suggestedAction(suggestedAction)
                .summary(summary)
                .strengths(strengths)
                .issues(issues)
                .recommendations(recommendations)
                .build();
    }
}
