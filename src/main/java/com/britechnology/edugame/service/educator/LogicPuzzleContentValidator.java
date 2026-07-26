package com.britechnology.edugame.service.educator;

import com.britechnology.edugame.exception.ApiException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Validation métier des puzzles logique (miroir des règles frontend).
 */
public final class LogicPuzzleContentValidator {

    private static final ObjectMapper MAPPER = new ObjectMapper();
    private static final Pattern HEX_COLOR = Pattern.compile("^#([0-9a-fA-F]{3}|[0-9a-fA-F]{6})$");
    private static final Pattern TYPE_JSON_PATTERN = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");

    private LogicPuzzleContentValidator() {
    }

    public static void validate(String sousType, String enonce, String bonneReponse, String donnees) {
        String safeEnonce = enonce != null ? enonce.trim() : "";
        if (safeEnonce.isEmpty()) {
            throw ApiException.badRequest("L'énoncé est requis");
        }

        String subtype = normalizeSubtype(sousType, donnees);
        switch (subtype) {
            case "SUITE_LOGIQUE" -> validateSuiteLogique(bonneReponse, donnees);
            case "COLOR_MATCH" -> validateColorMatch(bonneReponse, donnees);
            default -> validateOptionsChoice(bonneReponse, donnees);
        }
    }

    private static void validateSuiteLogique(String bonneReponse, String donnees) {
        if (bonneReponse == null || bonneReponse.isBlank()) {
            throw ApiException.badRequest("La bonne réponse est requise");
        }
        JsonNode root = parseDonnees(donnees);
        JsonNode sequence = root != null ? root.get("sequence") : null;
        if (sequence == null || !sequence.isArray()) {
            throw ApiException.badRequest("La suite doit contenir au moins 3 éléments");
        }
        int count = 0;
        for (JsonNode node : sequence) {
            if (node.isNull()) {
                continue;
            }
            String value = node.isTextual() ? node.asText() : node.toString();
            if (value != null && !value.isBlank()) {
                count++;
            }
        }
        if (count < 3) {
            throw ApiException.badRequest("La suite doit contenir au moins 3 éléments");
        }
    }

    private static void validateOptionsChoice(String bonneReponse, String donnees) {
        if (bonneReponse == null || bonneReponse.isBlank()) {
            throw ApiException.badRequest("La bonne réponse est requise");
        }
        List<String> options = extractOptions(donnees);
        if (options.size() < 2) {
            throw ApiException.badRequest("Ajoutez au moins 2 options");
        }
        String answer = bonneReponse.trim();
        boolean match = options.stream().anyMatch(opt -> opt.equalsIgnoreCase(answer));
        if (!match) {
            throw ApiException.badRequest("La bonne réponse doit correspondre à l'une des options");
        }
    }

    private static void validateColorMatch(String bonneReponse, String donnees) {
        List<ColorWordPair> pairs = parseColorPairs(donnees, bonneReponse);
        if (pairs.size() < 2) {
            throw ApiException.badRequest("Ajoutez au moins 2 liaisons couleur → mot");
        }

        Set<String> colors = new HashSet<>();
        Set<String> words = new HashSet<>();
        for (ColorWordPair pair : pairs) {
            if (!HEX_COLOR.matcher(pair.color).matches()) {
                throw ApiException.badRequest("Chaque couleur doit être un code hex valide");
            }
            if (pair.word.isBlank()) {
                throw ApiException.badRequest("Chaque couleur doit être reliée à un mot");
            }
            String colorKey = pair.color.toLowerCase(Locale.ROOT);
            String wordKey = pair.word.toLowerCase(Locale.ROOT);
            if (!colors.add(colorKey)) {
                throw ApiException.badRequest("Deux liaisons utilisent la même couleur");
            }
            if (!words.add(wordKey)) {
                throw ApiException.badRequest("Deux liaisons utilisent le même mot");
            }
        }

        if (bonneReponse == null || !bonneReponse.trim().startsWith("{")) {
            throw ApiException.badRequest("La bonne réponse doit décrire les liaisons couleur → mot");
        }

        try {
            JsonNode answerNode = MAPPER.readTree(bonneReponse);
            if (!answerNode.isObject()) {
                throw ApiException.badRequest("La bonne réponse COLOR_MATCH doit être un objet JSON");
            }
            Iterator<Map.Entry<String, JsonNode>> fields = answerNode.fields();
            int linkCount = 0;
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> entry = fields.next();
                String color = entry.getKey().trim().toLowerCase(Locale.ROOT);
                String word = entry.getValue().asText("").trim();
                if (!HEX_COLOR.matcher(color).matches() || word.isBlank()) {
                    throw ApiException.badRequest("Chaque liaison couleur → mot doit être valide");
                }
                linkCount++;
            }
            if (linkCount != pairs.size()) {
                throw ApiException.badRequest("La bonne réponse doit contenir toutes les liaisons couleur → mot");
            }
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.badRequest("La bonne réponse COLOR_MATCH est invalide");
        }
    }

    private static List<String> extractOptions(String donnees) {
        JsonNode root = parseDonnees(donnees);
        JsonNode options = root != null ? root.get("options") : null;
        List<String> cleaned = new ArrayList<>();
        if (options != null && options.isArray()) {
            for (JsonNode node : options) {
                String opt = node.asText("").trim();
                if (!opt.isEmpty()) {
                    cleaned.add(opt);
                }
            }
        }
        return cleaned;
    }

    private static List<ColorWordPair> parseColorPairs(String donnees, String bonneReponse) {
        List<ColorWordPair> pairs = new ArrayList<>();
        JsonNode root = parseDonnees(donnees);
        JsonNode colorPairs = root != null ? root.get("colorPairs") : null;
        if (colorPairs != null && colorPairs.isArray()) {
            for (JsonNode node : colorPairs) {
                String color = node.path("color").asText("").trim().toLowerCase(Locale.ROOT);
                String word = node.path("word").asText("").trim();
                if (!color.isEmpty() && !word.isEmpty()) {
                    pairs.add(new ColorWordPair(color, word));
                }
            }
        }
        if (!pairs.isEmpty()) {
            return pairs;
        }

        if (bonneReponse != null && bonneReponse.trim().startsWith("{")) {
            try {
                JsonNode answerNode = MAPPER.readTree(bonneReponse);
                if (answerNode.isObject()) {
                    Iterator<Map.Entry<String, JsonNode>> fields = answerNode.fields();
                    while (fields.hasNext()) {
                        Map.Entry<String, JsonNode> entry = fields.next();
                        String color = entry.getKey().trim().toLowerCase(Locale.ROOT);
                        String word = entry.getValue().asText("").trim();
                        if (!color.isEmpty() && !word.isEmpty()) {
                            pairs.add(new ColorWordPair(color, word));
                        }
                    }
                }
            } catch (Exception ignored) {
                // handled by validateColorMatch
            }
        }
        return pairs;
    }

    private static JsonNode parseDonnees(String donnees) {
        if (donnees == null || donnees.isBlank()) {
            return null;
        }
        try {
            return MAPPER.readTree(donnees);
        } catch (Exception e) {
            throw ApiException.badRequest("Les données du puzzle sont invalides");
        }
    }

    private static String normalizeSubtype(String rawSousType, String rawDonnees) {
        String candidate = rawSousType != null && !rawSousType.isBlank()
                ? rawSousType.trim().toUpperCase(Locale.ROOT)
                : extractTypeFromDonnees(rawDonnees);
        if (candidate == null || candidate.isBlank()) {
            return "DEDUCTION";
        }
        return switch (candidate) {
            case "SUITE_LOGIQUE", "INTRUS", "DEDUCTION", "COLOR_MATCH" -> candidate;
            default -> "DEDUCTION";
        };
    }

    private static String extractTypeFromDonnees(String rawDonnees) {
        if (rawDonnees == null || rawDonnees.isBlank()) {
            return null;
        }
        var matcher = TYPE_JSON_PATTERN.matcher(rawDonnees);
        return matcher.find() ? matcher.group(1) : null;
    }

    private record ColorWordPair(String color, String word) {
    }
}
