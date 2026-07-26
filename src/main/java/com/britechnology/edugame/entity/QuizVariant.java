package com.britechnology.edugame.entity;

/**
 * Variante pédagogique d'un jeu QUIZ (mono-variante : une variante par jeu).
 */
public enum QuizVariant {
    DEFAULT,
    TRUE_FALSE,
    CLOZE,
    IMAGE_WORD,
    SYNONYM_ANTONYM,
    COLOR_TRANSLATION,
    AUDIO_COLOR;

    public static QuizVariant fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return DEFAULT;
        }
        String normalized = raw.trim().toUpperCase();
        for (QuizVariant variant : values()) {
            if (variant.name().equals(normalized)) {
                return variant;
            }
        }
        return DEFAULT;
    }
}
