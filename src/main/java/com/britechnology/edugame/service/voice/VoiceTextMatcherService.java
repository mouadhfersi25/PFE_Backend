package com.britechnology.edugame.service.voice;

import com.britechnology.edugame.entity.VoiceTolerance;
import com.britechnology.edugame.repository.badge.NiveauRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class VoiceTextMatcherService {

    public record MatchResult(
            int scoreContenu,
            boolean reussite,
            List<String> expectedWords,
            List<String> spokenWords,
            List<String> correctWords,
            List<String> missedWords,
            List<String> extraWords
    ) {}

    public MatchResult compare(String referenceText, String spokenText, VoiceTolerance tolerance) {
        List<String> expected = tokenize(referenceText, tolerance);
        List<String> spoken = tokenize(spokenText, tolerance);

        if (expected.isEmpty()) {
            return new MatchResult(0, false, expected, spoken, List.of(), List.of(), spoken);
        }

        Set<String> spokenSet = new LinkedHashSet<>(spoken);
        List<String> correct = new ArrayList<>();
        List<String> missed = new ArrayList<>();

        for (String word : expected) {
            if (spokenSet.contains(word)) {
                correct.add(word);
            } else {
                missed.add(word);
            }
        }

        List<String> extra = new ArrayList<>();
        Set<String> expectedSet = new LinkedHashSet<>(expected);
        for (String word : spoken) {
            if (!expectedSet.contains(word)) {
                extra.add(word);
            }
        }

        int score = Math.round((correct.size() * 100f) / expected.size());
        boolean reussite = score >= successThreshold(tolerance);
        return new MatchResult(score, reussite, expected, spoken, correct, missed, extra);
    }

    private int successThreshold(VoiceTolerance tolerance) {
        return switch (tolerance != null ? tolerance : VoiceTolerance.NORMAL) {
            case STRICT -> 85;
            case SOUPLE -> 60;
            default -> 70;
        };
    }

    private List<String> tokenize(String text, VoiceTolerance tolerance) {
        if (text == null || text.isBlank()) {
            return List.of();
        }
        String normalized = normalize(text, tolerance);
        String[] parts = normalized.split("\\s+");
        List<String> out = new ArrayList<>();
        for (String part : parts) {
            String token = part.trim();
            if (!token.isEmpty()) {
                out.add(token);
            }
        }
        return out;
    }

    private String normalize(String text, VoiceTolerance tolerance) {
        String value = text.trim().toLowerCase(Locale.ROOT);
        value = value.replaceAll("[«»\"'’.,!?;:()\\[\\]{}…-]", " ");
        if (tolerance != VoiceTolerance.STRICT) {
            value = Normalizer.normalize(value, Normalizer.Form.NFD)
                    .replaceAll("\\p{M}+", "");
        }
        return value.replaceAll("\\s+", " ").trim();
    }
}
