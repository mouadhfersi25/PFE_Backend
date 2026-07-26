package com.britechnology.edugame.service.educator;

import com.britechnology.edugame.entity.Question;
import com.britechnology.edugame.exception.ApiException;

/**
 * Validation métier des questions quiz selon la variante du jeu.
 */
public final class QuizQuestionContentValidator {

    private QuizQuestionContentValidator() {
    }

    public static void validateVariantMedia(String sousType, String mediaUrl, String promptAudioUrl) {
        if ("IMAGE_WORD".equals(sousType)) {
            if (!isPresentMedia(mediaUrl, true)) {
                throw ApiException.badRequest("Une image est requise pour les questions Image + mot");
            }
        }
        if ("AUDIO_COLOR".equals(sousType)) {
            if (!isPresentMedia(promptAudioUrl, false)) {
                throw ApiException.badRequest("Un fichier audio est requis pour les questions Audio + couleur");
            }
        }
    }

    public static void validatePersistedQuestion(String sousType, Question question) {
        if (question == null) {
            throw ApiException.badRequest("Question invalide");
        }
        validateVariantMedia(sousType, question.getMediaUrl(), question.getPromptAudioUrl());
    }

    private static boolean isPresentMedia(String url, boolean image) {
        if (url == null || url.isBlank()) {
            return false;
        }
        String value = url.trim();
        if (value.startsWith("http://") || value.startsWith("https://")) {
            return true;
        }
        if (image) {
            return value.startsWith("data:image/");
        }
        return value.startsWith("data:audio/");
    }
}
