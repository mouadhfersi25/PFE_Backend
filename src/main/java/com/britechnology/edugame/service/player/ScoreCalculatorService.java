package com.britechnology.edugame.service.player;

import com.britechnology.edugame.dto.player.CreateGameSessionRequest;
import com.britechnology.edugame.entity.EtatSession;
import com.britechnology.edugame.entity.TypeJeu;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@Slf4j
public class ScoreCalculatorService {
    public static final String SCORING_RULES_VERSION = "v1.3.0";

    public ScoreCalculationResult calculate(
            TypeJeu typeJeu,
            EtatSession etatSession,
            CreateGameSessionRequest request,
            int durationSeconds,
            Integer gameDifficultyRaw
    ) {
        List<String> anomalies = new ArrayList<>();
        int safeDuration = normalizeDuration(typeJeu, etatSession, durationSeconds, anomalies);
        validateInputCoherence(typeJeu, etatSession, request, anomalies);
        int gameDifficulty = clampDifficulty(gameDifficultyRaw);
        double difficultyMultiplier = 1.0 + (gameDifficulty * 0.06);

        int score;
        if (etatSession == EtatSession.EN_COURS) {
            score = 0;
        } else if (etatSession == EtatSession.ABANDONNE) {
            score = (int) Math.round(calculateTerminatedScore(typeJeu, request, safeDuration, anomalies) * 0.4);
            anomalies.add("abandon_penalty_applied");
        } else {
            score = calculateTerminatedScore(typeJeu, request, safeDuration, anomalies);
        }

        score = (int) Math.round(score * difficultyMultiplier);
        int xpGained = Math.max(0, (int) Math.round(score * 0.18 + gameDifficulty));
        if (Boolean.TRUE.equals(request.getReussite())) xpGained += 4;
        xpGained = Math.min(60, xpGained);

        int baseScore = clampByGameType(typeJeu, score);
        if (baseScore != score) {
            anomalies.add("score_clamped");
        }

        if (!anomalies.isEmpty()) {
            log.warn("Scoring anomalies detected type={} etat={} anomalies={}", typeJeu, etatSession, anomalies);
        }
        return new ScoreCalculationResult(baseScore, xpGained, safeDuration, SCORING_RULES_VERSION, String.join(",", anomalies));
    }

    private int calculateTerminatedScore(TypeJeu typeJeu, CreateGameSessionRequest request, int durationSeconds, List<String> anomalies) {
        if (typeJeu == TypeJeu.QUIZ) {
            return scoreQuiz(request, durationSeconds, anomalies);
        }
        if (typeJeu == TypeJeu.MEMOIRE) {
            return scoreMemory(request, durationSeconds, anomalies);
        }
        if (typeJeu == TypeJeu.LOGIQUE) {
            return scoreLogic(request, durationSeconds, anomalies);
        }
        return scoreReflex(request, durationSeconds, anomalies);
    }

    private int scoreQuiz(CreateGameSessionRequest request, int durationSeconds, List<String> anomalies) {
        Integer total = sanitizeNonNegative(request.getTotalQuestions());
        Integer correct = sanitizeNonNegative(request.getCorrectAnswers());
        Integer accuracy = sanitizePercentage(request.getAccuracyPercent());

        if (total == null || total == 0) {
            if (accuracy != null) {
                total = 10;
                correct = (int) Math.round((accuracy / 100.0) * total);
                anomalies.add("quiz_totalQuestions_inferred");
            } else {
                total = 10;
                correct = 0;
                anomalies.add("quiz_metrics_missing");
            }
        } else if (correct == null) {
            correct = accuracy != null ? (int) Math.round((accuracy / 100.0) * total) : 0;
            anomalies.add("quiz_correctAnswers_inferred");
        }

        correct = Math.min(correct, total);
        int wrong = Math.max(0, total - correct);
        int precisionPercent = Math.round((correct * 100.0f) / Math.max(1, total));
        int precisionComponent = precisionPercent * 2;
        int penalty = wrong * 8;
        int speedBonus = Math.max(0, 120 - (durationSeconds / 2));
        return precisionComponent + speedBonus - penalty;
    }

    private int scoreMemory(CreateGameSessionRequest request, int durationSeconds, List<String> anomalies) {
        int moves = valueOrDefault(sanitizeNonNegative(request.getMoves()), 0);
        int matches = valueOrDefault(sanitizeNonNegative(request.getMatches()), 0);
        if (moves > 0 && matches > moves) {
            matches = moves;
            anomalies.add("memory_matches_clamped_to_moves");
        }
        int efficiencyPercent = moves > 0 ? (int) Math.round((matches * 100.0) / moves) : 0;
        int precisionComponent = efficiencyPercent * 2;
        int errorPenalty = Math.max(0, moves - matches) * 5;
        int timePenalty = durationSeconds / 4;
        return 60 + precisionComponent - errorPenalty - timePenalty;
    }

    private int scoreLogic(CreateGameSessionRequest request, int durationSeconds, List<String> anomalies) {
        int attempts = valueOrDefault(sanitizeNonNegative(request.getAttempts()), 1);
        int hints = valueOrDefault(sanitizeNonNegative(request.getHintsUsed()), 0);
        if (hints > attempts) {
            anomalies.add("logic_hints_gt_attempts");
        }
        int precisionComponent = Math.max(0, 120 - ((attempts - 1) * 18));
        int hintsPenalty = hints * 15;
        int timePenalty = durationSeconds / 5;
        return 80 + precisionComponent - hintsPenalty - timePenalty;
    }

    private int scoreReflex(CreateGameSessionRequest request, int durationSeconds, List<String> anomalies) {
        int totalRounds = valueOrDefault(sanitizeNonNegative(request.getTotalRounds()), 10);
        int successfulRounds = valueOrDefault(sanitizeNonNegative(request.getSuccessfulRounds()), 0);
        if (successfulRounds > totalRounds) {
            successfulRounds = totalRounds;
            anomalies.add("reflex_successfulRounds_clamped");
        }
        int reactionTime = valueOrDefault(sanitizeNonNegative(request.getReactionTimeMs()), 1000);
        int precisionPercent = (int) Math.round((successfulRounds * 100.0) / Math.max(1, totalRounds));
        int precisionComponent = precisionPercent * 2;
        int speedBonus = Math.max(0, 140 - (reactionTime / 12));
        int errorPenalty = Math.max(0, totalRounds - successfulRounds) * 6;
        int durationPenalty = durationSeconds / 5;
        return precisionComponent + speedBonus - errorPenalty - durationPenalty;
    }

    private int normalizeDuration(TypeJeu typeJeu, EtatSession etatSession, int durationSeconds, List<String> anomalies) {
        int safeDuration = Math.max(0, durationSeconds);
        int minDuration = switch (typeJeu) {
            case QUIZ, LOGIQUE, MEMOIRE, REFLEXE -> 5;
        };
        int maxDuration = switch (typeJeu) {
            case QUIZ, LOGIQUE, MEMOIRE, REFLEXE -> 4 * 60 * 60;
        };

        if (etatSession == EtatSession.TERMINE && safeDuration < minDuration) {
            anomalies.add("duration_too_short_clamped");
            safeDuration = minDuration;
        }
        if (safeDuration > maxDuration) {
            anomalies.add("duration_too_long_clamped");
            safeDuration = maxDuration;
        }
        return safeDuration;
    }

    private void validateInputCoherence(TypeJeu typeJeu, EtatSession etatSession, CreateGameSessionRequest request, List<String> anomalies) {
        if (request.getTotalQuestions() != null && request.getCorrectAnswers() != null
                && request.getCorrectAnswers() > request.getTotalQuestions()) {
            throw new IllegalArgumentException("correctAnswers ne peut pas dépasser totalQuestions");
        }
        if (request.getTotalRounds() != null && request.getSuccessfulRounds() != null
                && request.getSuccessfulRounds() > request.getTotalRounds()) {
            throw new IllegalArgumentException("successfulRounds ne peut pas dépasser totalRounds");
        }
        if (typeJeu == TypeJeu.MEMOIRE && request.getMoves() != null && request.getMoves() < 0) {
            throw new IllegalArgumentException("moves ne peut pas être négatif");
        }
        if (request.getAccuracyPercent() != null && (request.getAccuracyPercent() < 0 || request.getAccuracyPercent() > 100)) {
            anomalies.add("accuracy_out_of_range_clamped");
        }
        if (etatSession == EtatSession.EN_COURS) {
            boolean hasTerminalMetrics = request.getCorrectAnswers() != null
                    || request.getTotalQuestions() != null
                    || request.getTotalRounds() != null
                    || request.getSuccessfulRounds() != null
                    || request.getMoves() != null
                    || request.getAttempts() != null;
            if (hasTerminalMetrics) {
                throw new IllegalArgumentException("etatSession EN_COURS incohérent avec des métriques de fin de partie");
            }
        }
    }

    private int clampByGameType(TypeJeu typeJeu, int score) {
        int minScore = 0;
        int maxScore = switch (typeJeu) {
            case QUIZ -> 400;
            case MEMOIRE -> 350;
            case LOGIQUE -> 300;
            case REFLEXE -> 300;
        };
        return Math.max(minScore, Math.min(maxScore, score));
    }

    private int clampDifficulty(Integer difficultyRaw) {
        if (difficultyRaw == null) return 5;
        return Math.max(0, Math.min(10, difficultyRaw));
    }

    private Integer sanitizeNonNegative(Integer value) {
        if (value == null) return null;
        return Math.max(0, value);
    }

    private Integer sanitizePercentage(Integer value) {
        if (value == null) return null;
        return Math.max(0, Math.min(100, value));
    }

    private int valueOrDefault(Integer value, int fallback) {
        return value != null ? value : fallback;
    }

    public record ScoreCalculationResult(
            int baseScore,
            int xpFromDeterministic,
            int normalizedDurationSeconds,
            String scoringRulesVersion,
            String anomalyNotes
    ) {}
}
