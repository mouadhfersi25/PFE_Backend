package com.britechnology.edugame.service.quiz;

import com.britechnology.edugame.entity.Question;
import com.britechnology.edugame.entity.QuizVariant;

/**
 * Résolution centralisée de la variante quiz (mono-variante par jeu).
 */
public final class QuizVariantSupport {

    private QuizVariantSupport() {
    }

    public static String resolveCode(QuizVariant gameVariant, Question question) {
        String gameCode = gameVariant != null ? gameVariant.name() : QuizVariant.DEFAULT.name();
        if (question == null) {
            return gameCode;
        }
        String stored = normalizeCode(question.getSousType());
        if (!QuizVariant.DEFAULT.name().equals(stored)) {
            return stored;
        }
        if (QuizVariant.DEFAULT.name().equals(gameCode) && question.getContenu() != null && question.getContenu().contains("___")) {
            return QuizVariant.CLOZE.name();
        }
        return gameCode;
    }

    public static String normalizeCode(String raw) {
        return QuizVariant.fromCode(raw).name();
    }
}
