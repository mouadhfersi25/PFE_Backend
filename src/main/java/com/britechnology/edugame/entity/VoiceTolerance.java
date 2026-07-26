package com.britechnology.edugame.entity;

public enum VoiceTolerance {
    STRICT,
    NORMAL,
    SOUPLE;

    public static VoiceTolerance fromCode(String raw) {
        if (raw == null || raw.isBlank()) {
            return NORMAL;
        }
        try {
            return valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException ex) {
            return NORMAL;
        }
    }
}
