package com.britechnology.edugame.service.voice;

import com.britechnology.edugame.entity.EtatVoiceSeries;
import com.britechnology.edugame.entity.VoiceSeries;
import com.britechnology.edugame.exception.ApiException;

import java.util.Arrays;
import java.util.Locale;

public final class VoiceContentValidator {

    private VoiceContentValidator() {
    }

    public static void validateSeriesForPublish(VoiceSeries series, long promptsCount) {
        if (series == null) {
            throw ApiException.badRequest("Série introuvable");
        }
        if (series.getTitre() == null || series.getTitre().isBlank()) {
            throw ApiException.badRequest("Le titre de la série est requis");
        }
        if (promptsCount < 1) {
            throw ApiException.badRequest("Ajoutez au moins une consigne avant de publier");
        }
    }

    public static void validatePromptText(String texteReference) {
        String text = texteReference != null ? texteReference.trim() : "";
        if (text.isEmpty()) {
            throw ApiException.badRequest("Le texte de référence est requis");
        }
        long wordCount = Arrays.stream(text.split("\\s+"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .count();
        if (wordCount < 2) {
            throw ApiException.badRequest("Le texte doit contenir au moins 2 mots");
        }
    }

    public static void requireDraft(VoiceSeries series) {
        if (series.getEtat() != EtatVoiceSeries.BROUILLON) {
            throw ApiException.badRequest("Seules les séries en brouillon peuvent être modifiées");
        }
    }

    public static String normalizeLangue(String raw) {
        if (raw == null || raw.isBlank()) {
            return "fr";
        }
        return raw.trim().toLowerCase(Locale.ROOT);
    }
}
