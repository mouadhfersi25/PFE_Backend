package com.stage.auth.authbackend.service.player;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stage.auth.authbackend.dto.player.CreateGameSessionRequest;
import com.stage.auth.authbackend.entity.SessionJeu;
import com.stage.auth.authbackend.entity.TypeJeu;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SmartAdjustmentService {
    private static final double MIN_ADJUSTMENT = -0.08;
    private static final double MAX_ADJUSTMENT = 0.08;

    private final GroqClientService groqClientService;
    private final ObjectMapper objectMapper;

    public AdjustmentResult computeBoundedAdjustment(
            Long userId,
            TypeJeu typeJeu,
            Integer difficulty,
            int baseScore,
            CreateGameSessionRequest request,
            List<SessionJeu> recentSessions
    ) {
        List<String> flags = new ArrayList<>();
        double statistical = computeStatisticalSignal(baseScore, recentSessions, flags);
        if (!groqClientService.isReady()) {
            flags.add("groq_unavailable");
            return new AdjustmentResult(clamp(statistical), "fallback_statistical", "fallback_no_groq", String.join(",", flags));
        }

        try {
            String response = groqClientService.requestJson(systemPrompt(), userPrompt(typeJeu, difficulty, baseScore, request, recentSessions));
            if (response == null || response.isBlank()) {
                flags.add("groq_empty_response");
                return new AdjustmentResult(clamp(statistical), "fallback_statistical", "fallback_empty_response", String.join(",", flags));
            }
            JsonNode root = objectMapper.readTree(response);
            double aiAdjustment = root.path("adjustment").asDouble(statistical);
            String explanationCode = safeString(root.path("explanationCode").asText("llm_default"));
            String riskFlags = safeString(root.path("riskFlags").asText(""));
            double merged = clamp((aiAdjustment * 0.7) + (statistical * 0.3));
            if (aiAdjustment < MIN_ADJUSTMENT || aiAdjustment > MAX_ADJUSTMENT) {
                flags.add("llm_adjustment_clamped");
            }
            if (!riskFlags.isBlank()) flags.add(riskFlags);
            return new AdjustmentResult(merged, "groq_hybrid", explanationCode, String.join(",", flags));
        } catch (Exception ex) {
            log.warn("Smart adjustment parse failed, fallback enabled: {}", ex.getMessage());
            flags.add("groq_parse_failed");
            return new AdjustmentResult(clamp(statistical), "fallback_statistical", "fallback_parse_failure", String.join(",", flags));
        }
    }

    private double computeStatisticalSignal(int baseScore, List<SessionJeu> recentSessions, List<String> flags) {
        if (recentSessions == null || recentSessions.isEmpty()) return 0.0;
        double avg = recentSessions.stream()
                .mapToInt(s -> s.getScoreFinal() != null ? s.getScoreFinal() : (s.getScoreGlobal() != null ? s.getScoreGlobal() : 0))
                .average()
                .orElse(0.0);
        if (avg <= 0) return 0.0;
        double deltaRatio = (baseScore - avg) / Math.max(1.0, avg);
        double adjustment = Math.max(-0.04, Math.min(0.04, deltaRatio * 0.05));
        if (Math.abs(deltaRatio) > 0.7) flags.add("statistical_outlier_detected");
        return adjustment;
    }

    private String systemPrompt() {
        return """
                You evaluate educational game session quality.
                Return JSON only with keys:
                adjustment (number between -0.08 and 0.08),
                explanationCode (short snake_case),
                riskFlags (comma-separated short codes).
                Never exceed bounds.
                """;
    }

    private String userPrompt(
            TypeJeu typeJeu,
            Integer difficulty,
            int baseScore,
            CreateGameSessionRequest request,
            List<SessionJeu> recentSessions
    ) {
        int recentCount = recentSessions == null ? 0 : recentSessions.size();
        return """
                gameType=%s
                difficulty=%s
                baseScore=%d
                accuracyPercent=%s
                reactionTimeMs=%s
                totalQuestions=%s
                correctAnswers=%s
                moves=%s
                matches=%s
                attempts=%s
                hintsUsed=%s
                totalRounds=%s
                successfulRounds=%s
                recentSessions=%d
                """.formatted(
                typeJeu,
                difficulty == null ? "5" : difficulty,
                baseScore,
                value(request.getAccuracyPercent()),
                value(request.getReactionTimeMs()),
                value(request.getTotalQuestions()),
                value(request.getCorrectAnswers()),
                value(request.getMoves()),
                value(request.getMatches()),
                value(request.getAttempts()),
                value(request.getHintsUsed()),
                value(request.getTotalRounds()),
                value(request.getSuccessfulRounds()),
                recentCount
        );
    }

    private String value(Integer val) {
        return val == null ? "null" : String.valueOf(val);
    }

    private double clamp(double value) {
        return Math.max(MIN_ADJUSTMENT, Math.min(MAX_ADJUSTMENT, value));
    }

    private String safeString(String value) {
        if (value == null) return "";
        String out = value.trim();
        return out.length() > 60 ? out.substring(0, 60) : out;
    }

    public record AdjustmentResult(double adjustment, String source, String explanationCode, String riskFlags) {}
}
