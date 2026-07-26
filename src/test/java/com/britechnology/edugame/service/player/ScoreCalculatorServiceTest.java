package com.britechnology.edugame.service.player;

import com.britechnology.edugame.dto.player.CreateGameSessionRequest;
import com.britechnology.edugame.entity.EtatSession;
import com.britechnology.edugame.entity.TypeJeu;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ScoreCalculatorServiceTest {

    private final ScoreCalculatorService service = new ScoreCalculatorService();

    @Test
    void shouldComputeQuizBaseScoreAndVersion() {
        CreateGameSessionRequest request = CreateGameSessionRequest.builder()
                .totalQuestions(10)
                .correctAnswers(8)
                .accuracyPercent(80)
                .reussite(true)
                .build();

        ScoreCalculatorService.ScoreCalculationResult result = service.calculate(
                TypeJeu.QUIZ,
                EtatSession.TERMINE,
                request,
                90,
                5
        );

        assertTrue(result.baseScore() > 0);
        assertTrue(result.xpFromDeterministic() > 0);
        assertEquals("v1.3.0", result.scoringRulesVersion());
    }

    @Test
    void shouldApplyAbandonPenalty() {
        CreateGameSessionRequest request = CreateGameSessionRequest.builder()
                .totalRounds(10)
                .successfulRounds(10)
                .reactionTimeMs(300)
                .build();

        ScoreCalculatorService.ScoreCalculationResult abandoned = service.calculate(
                TypeJeu.REFLEXE,
                EtatSession.ABANDONNE,
                request,
                60,
                4
        );
        ScoreCalculatorService.ScoreCalculationResult terminated = service.calculate(
                TypeJeu.REFLEXE,
                EtatSession.TERMINE,
                request,
                60,
                4
        );

        assertTrue(abandoned.baseScore() < terminated.baseScore());
        assertTrue(abandoned.anomalyNotes().contains("abandon_penalty_applied"));
    }
}
