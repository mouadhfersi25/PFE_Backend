package com.britechnology.edugame.service.educator;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.britechnology.edugame.dto.educator.QuizQuestionDTO;
import com.britechnology.edugame.entity.Jeu;
import com.britechnology.edugame.entity.TypeJeu;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.game.JeuRepository;
import com.britechnology.edugame.service.player.GroqClientService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpStatusCodeException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AiQuizGenerationService {

    private static final String GEMINI_BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models";

    private final ObjectMapper objectMapper;
    private final JeuRepository jeuRepository;
    private final GroqClientService groqClientService;
    private final GameCoverService gameCoverService;
    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${ai.gemini.api-key:}")
    private String apiKey;

    @Value("${ai.gemini.model:gemini-1.5-flash}")
    private String model;

    @Value("${ai.gemini.models:}")
    private String configuredModels;

    @Value("${ai.gemini.enabled:false}")
    private boolean enabled;

    @Value("${ai.gemini.timeout-ms:10000}")
    private int timeoutMs;

    /** Timeouts dédiés génération quiz (évite d’attendre 2 modèles × 2 essais × 10s). */
    @Value("${ai.quiz.connect-timeout-ms:1800}")
    private int quizConnectTimeoutMs;

    @Value("${ai.quiz.read-timeout-ms:6500}")
    private int quizReadTimeoutMs;

    @Value("${ai.quiz.gemini-attempts-per-model:1}")
    private int quizAttemptsPerModel;

    /** Nombre max de modèles Gemini essayés en série pour le quiz (1 = plus rapide). */
    @Value("${ai.quiz.max-gemini-models:1}")
    private int quizMaxGeminiModels;

    public List<QuizQuestionDTO> generatePreview(Long gameId, Integer requestedCount, String requestedSousType) {
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

        final String mode = normalizeSousTypeMode(
                requestedSousType != null && !requestedSousType.isBlank()
                        ? requestedSousType
                        : (jeu.getQuizVariant() != null ? jeu.getQuizVariant().name() : "DEFAULT")
        );
        String prompt = buildPrompt(jeu, count, mode);
        String responseText;
        try {
            responseText = callGeminiWithRetry(prompt);
        } catch (ApiException ex) {
            if (ex.getStatus() == HttpStatus.SERVICE_UNAVAILABLE || ex.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR) {
                responseText = callGroqFallback(jeu, count, mode);
            } else {
                throw ex;
            }
        }

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
                String sousType = "MIXED".equals(mode)
                        ? normalizeQuestionSousType(cleanText(q.path("sousType").asText(null)))
                        : mode;

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

                options = normalizeOptionsForSousType(sousType, options, bonneReponse);
                if (!isValidGeneratedQuestion(sousType, contenu, bonneReponse, options)) {
                    continue;
                }

                QuizQuestionDTO.QuizQuestionDTOBuilder builder = QuizQuestionDTO.builder()
                        .jeuId(jeu.getId())
                        .jeuTitre(jeu.getTitre())
                        .contenu(contenu)
                        .bonneReponse(bonneReponse)
                        .options(options.isEmpty() ? null : options)
                        .sousType(sousType)
                        .explication(explication)
                        .difficulte(difficulte);

                if ("IMAGE_WORD".equals(sousType)) {
                    String illustration = gameCoverService.generateEducationalIllustrationDataUrl(bonneReponse);
                    if (illustration != null) {
                        builder.mediaUrl(illustration);
                    }
                }

                generated.add(builder.build());
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
            configurePingTimeouts();
            String selectedModel = resolveCandidateModels().get(0);
            String url = GEMINI_BASE_URL + "/" + selectedModel + ":generateContent?key=" + apiKey;
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
                    "model", selectedModel,
                    "reply", text.trim()
            );
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw toAiServiceException(e, "Gemini ping failed");
        }
    }

    private String callGemini(String prompt, String modelName) {
        try {
            configureQuizGenerationTimeouts();
            String url = GEMINI_BASE_URL + "/" + modelName + ":generateContent?key=" + apiKey;
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
            throw toAiServiceException(e, "Gemini call failed (model=" + modelName + ")");
        }
    }

    private String callGeminiWithRetry(String prompt) {
        int attemptsPerModel = Math.max(1, quizAttemptsPerModel);
        ApiException last = null;
        List<String> fullChain = resolveCandidateModels();
        int cap = Math.max(1, quizMaxGeminiModels);
        List<String> models = fullChain.size() <= cap ? fullChain : fullChain.subList(0, cap);

        for (String candidateModel : models) {
            for (int attempt = 1; attempt <= attemptsPerModel; attempt++) {
                try {
                    return callGemini(prompt, candidateModel);
                } catch (ApiException ex) {
                    last = ex;
                    if (ex.getStatus() == HttpStatus.BAD_REQUEST) {
                        throw ex;
                    }
                    if (ex.getStatus() != HttpStatus.SERVICE_UNAVAILABLE || attempt == attemptsPerModel) {
                        break;
                    }
                    try {
                        Thread.sleep(120L * attempt);
                    } catch (InterruptedException ie) {
                        Thread.currentThread().interrupt();
                        throw ex;
                    }
                }
            }
        }

        throw last == null ? ApiException.serviceUnavailable("Service IA temporairement indisponible. Réessayez dans quelques instants.") : last;
    }

    private String callGroqFallback(Jeu jeu, int count, String mode) {
        String systemPrompt = """
                Tu es un assistant pédagogique expert.
                Tu retournes uniquement du JSON valide.
                """;
        String userPrompt = buildPrompt(jeu, count, mode);
        String content = groqClientService.requestJson(systemPrompt, userPrompt);
        if (content == null || content.isBlank()) {
            throw ApiException.serviceUnavailable("Service IA temporairement indisponible. Réessayez dans quelques instants.");
        }
        return content;
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

    private String normalizeSousTypeMode(String requestedSousType) {
        if (requestedSousType == null || requestedSousType.isBlank()) {
            return "DEFAULT";
        }
        String normalized = requestedSousType.trim().toUpperCase();
        return switch (normalized) {
            case "DEFAULT", "TRUE_FALSE", "CLOZE", "IMAGE_WORD",
                 "SYNONYM_ANTONYM", "COLOR_TRANSLATION", "AUDIO_COLOR", "MIXED" -> normalized;
            default -> "DEFAULT";
        };
    }

    private String normalizeQuestionSousType(String value) {
        if (value == null || value.isBlank()) {
            return "DEFAULT";
        }
        String normalized = value.trim().toUpperCase();
        return switch (normalized) {
            case "TRUE_FALSE", "CLOZE", "IMAGE_WORD", "SYNONYM_ANTONYM",
                 "COLOR_TRANSLATION", "AUDIO_COLOR" -> normalized;
            default -> "DEFAULT";
        };
    }

    private List<String> normalizeOptionsForSousType(String sousType, List<String> options, String bonneReponse) {
        if ("TRUE_FALSE".equals(sousType)) {
            return List.of("Vrai", "Faux");
        }
        List<String> normalized = new ArrayList<>(options);
        if (!normalized.isEmpty() && normalized.stream().noneMatch(o -> o.equalsIgnoreCase(bonneReponse))) {
            normalized.add(bonneReponse);
        }
        return normalized;
    }

    private boolean isValidGeneratedQuestion(String sousType, String contenu, String bonneReponse, List<String> options) {
        if ("TRUE_FALSE".equals(sousType)) {
            return bonneReponse.equalsIgnoreCase("Vrai") || bonneReponse.equalsIgnoreCase("Faux");
        }
        if ("CLOZE".equals(sousType)) {
            return contenu.contains("___")
                    && options.size() >= 3
                    && options.size() <= 6
                    && options.stream().anyMatch(o -> o.equalsIgnoreCase(bonneReponse));
        }
        if (options.isEmpty() || options.size() < 2) {
            return false;
        }
        return options.stream().anyMatch(o -> o.equalsIgnoreCase(bonneReponse));
    }

    private String buildPrompt(Jeu jeu, int count, String mode) {
        Integer diff = normalizeDifficulty(jeu.getDifficulte());
        String ageRange = (jeu.getAgeMin() != null || jeu.getAgeMax() != null)
                ? (String.valueOf(jeu.getAgeMin() == null ? "?" : jeu.getAgeMin()) + "-" + (jeu.getAgeMax() == null ? "?" : jeu.getAgeMax()))
                : "non précisée";
        String duration = jeu.getDureeMinutes() == null ? "non précisée" : jeu.getDureeMinutes() + " minutes";
        String description = (jeu.getDescription() == null || jeu.getDescription().isBlank()) ? "Aucune description fournie." : jeu.getDescription().trim();
        String variantRules = variantRulesForMode(mode);

        return """
                Tu es un expert en pédagogie. Génère des questions de quiz en français pour enfants/ados selon les contraintes.

                CONTRAINTES:
                - Titre du quiz: %s
                - Description: %s
                - Difficulté (0-10): %d
                - Tranche d'âge: %s
                - Durée visée: %s
                - Nombre de questions: %d
                - Questions variées, claires, non ambiguës, formulations courtes.
                - Réponses adaptées au niveau d'âge.
                - Éviter toute question dangereuse, violente, ou inadaptée.

                VARIANTE DEMANDÉE: %s
                %s

                FORMAT OBLIGATOIRE:
                Retourne UNIQUEMENT un JSON valide (sans texte avant/après), au format:
                {
                  "questions": [
                    {
                      "contenu": "...",
                      "options": ["...", "..."],
                      "bonneReponse": "...",
                      "explication": "Une phrase courte.",
                      "difficulte": 0%s
                    }
                  ]
                }
                """.formatted(
                jeu.getTitre(),
                description,
                diff,
                ageRange,
                duration,
                count,
                mode,
                variantRules,
                "MIXED".equals(mode) ? ",\n      \"sousType\": \"TRUE_FALSE\"" : ""
        );
    }

    private String variantRulesForMode(String mode) {
        return switch (mode) {
            case "TRUE_FALSE" -> """
                    - Chaque question est une affirmation claire (pas une question ouverte).
                    - options = exactement ["Vrai", "Faux"].
                    - bonneReponse = "Vrai" ou "Faux".
                    - explication = une phrase max.
                    """;
            case "CLOZE" -> """
                    - Chaque contenu est une phrase avec exactement ___ pour le mot manquant.
                    - options = 3 à 6 mots courts (un seul correct).
                    - bonneReponse = le mot qui remplit ___.
                    """;
            case "IMAGE_WORD" -> """
                    - contenu = "Quel mot correspond à cette image ?" (ou variante courte).
                    - bonneReponse = un mot concret illustrable (animal, objet, fruit...).
                    - options = exactement 4 mots distincts dont la bonne réponse.
                    """;
            case "SYNONYM_ANTONYM" -> """
                    - contenu demande un synonyme OU un antonyme d'un mot mis en évidence.
                    - options = exactement 4 mots.
                    - bonneReponse = l'unique bonne option.
                    """;
            case "COLOR_TRANSLATION" -> """
                    - contenu = nom d'une couleur en français (ex: "Quelle est la traduction de Rouge ?").
                    - options = exactement 4 traductions anglaises de couleurs.
                    - bonneReponse = traduction anglaise correcte.
                    """;
            case "AUDIO_COLOR" -> """
                    - contenu = "Quelle couleur entendez-vous ?" ou "Identifiez la couleur".
                    - options = exactement 4 noms de couleurs en français.
                    - bonneReponse = la couleur correcte en français.
                    """;
            case "MIXED" -> """
                    - Varie les sous-types: TRUE_FALSE, CLOZE, IMAGE_WORD, SYNONYM_ANTONYM, COLOR_TRANSLATION, AUDIO_COLOR, DEFAULT.
                    - Chaque objet question DOIT inclure "sousType" avec une de ces valeurs.
                    - Respecte les règles de chaque sous-type (CLOZE avec ___, TRUE_FALSE avec Vrai/Faux, etc.).
                    """;
            default -> """
                    - Chaque question: exactement 4 options (texte court); explication = une phrase max.
                    - QCM classique à choix multiples.
                    """;
        };
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

    private ApiException toAiServiceException(Exception e, String logPrefix) {
        if (e instanceof HttpStatusCodeException httpEx) {
            int status = httpEx.getStatusCode().value();
            String responseBody = httpEx.getResponseBodyAsString();
            log.warn("{}: status={} body={}", logPrefix, status, responseBody);

            if (status == 429 || status == 503 || containsUnavailableHint(responseBody)) {
                return ApiException.serviceUnavailable("Service IA temporairement indisponible. Réessayez dans quelques instants.");
            }
            if (status == 401 || status == 403) {
                return ApiException.badRequest("Service IA non autorisé. Vérifiez la configuration de la clé API.");
            }
            if (status >= 500) {
                return ApiException.serviceUnavailable("Service IA temporairement indisponible. Réessayez dans quelques instants.");
            }
            return ApiException.internalServerError("Échec du service IA externe.");
        }

        if (e instanceof ResourceAccessException) {
            log.warn("{}: timeout/network issue={}", logPrefix, e.getMessage());
            return ApiException.serviceUnavailable("Service IA temporairement indisponible. Réessayez dans quelques instants.");
        }

        log.warn("{}: {}", logPrefix, e.getMessage());
        return ApiException.internalServerError("Échec de la génération IA.");
    }

    private boolean containsUnavailableHint(String body) {
        if (body == null || body.isBlank()) return false;
        String normalized = body.toLowerCase();
        return normalized.contains("unavailable")
                || normalized.contains("currently experiencing high demand")
                || normalized.contains("temporarily");
    }

    private List<String> resolveCandidateModels() {
        Set<String> models = new LinkedHashSet<>();
        if (configuredModels != null && !configuredModels.isBlank()) {
            for (String value : configuredModels.split(",")) {
                String trimmed = value == null ? "" : value.trim();
                if (!trimmed.isEmpty()) {
                    models.add(trimmed);
                }
            }
        }
        if (model != null && !model.isBlank()) {
            models.add(model.trim());
        }
        models.add("gemini-1.5-flash");
        return new ArrayList<>(models);
    }

    private void configurePingTimeouts() {
        int connect = Math.min(2_000, Math.max(500, timeoutMs / 5));
        int read = Math.min(12_000, Math.max(3_000, timeoutMs));
        applyRequestTimeouts(connect, read);
    }

    private void configureQuizGenerationTimeouts() {
        int connect = Math.max(500, quizConnectTimeoutMs);
        int read = Math.max(1_500, quizReadTimeoutMs);
        applyRequestTimeouts(connect, read);
    }

    private void applyRequestTimeouts(int connectMs, int readMs) {
        if (restTemplate.getRequestFactory() instanceof SimpleClientHttpRequestFactory factory) {
            factory.setConnectTimeout(connectMs);
            factory.setReadTimeout(readMs);
            return;
        }
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(connectMs);
        factory.setReadTimeout(readMs);
        restTemplate.setRequestFactory(factory);
    }

}
