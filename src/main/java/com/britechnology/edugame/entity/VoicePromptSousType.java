package com.britechnology.edugame.entity;

public enum VoicePromptSousType {
    READ_ALOUD,
    REPEAT_AFTER;

    public static VoicePromptSousType fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return READ_ALOUD;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return READ_ALOUD;
        }
    }
}
