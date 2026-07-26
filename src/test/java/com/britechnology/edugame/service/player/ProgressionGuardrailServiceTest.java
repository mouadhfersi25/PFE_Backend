package com.britechnology.edugame.service.player;

import com.britechnology.edugame.dto.player.CreateGameSessionRequest;
import com.britechnology.edugame.entity.EtatSession;
import com.britechnology.edugame.entity.TypeJeu;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProgressionGuardrailServiceTest {

    private final ProgressionGuardrailService service = new ProgressionGuardrailService();

    @Test
    void shouldBlockLevelUpForAbandonedSession() {
        CreateGameSessionRequest request = CreateGameSessionRequest.builder().reussite(false).build();
        ProgressionGuardrailService.GuardrailResult result = service.apply(
                TypeJeu.QUIZ,
                EtatSession.ABANDONNE,
                request,
                40,
                0
        );
        assertEquals(0, result.xpAfterGuardrails());
        assertFalse(result.allowLevelUp());
    }

    @Test
    void shouldApplyAntiFarmDecay() {
        CreateGameSessionRequest request = CreateGameSessionRequest.builder()
                .accuracyPercent(90)
                .reussite(true)
                .build();

        ProgressionGuardrailService.GuardrailResult result = service.apply(
                TypeJeu.QUIZ,
                EtatSession.TERMINE,
                request,
                40,
                5
        );
        assertTrue(result.xpAfterGuardrails() < 40);
        assertTrue(result.guardrailNotes().contains("anti_farm_decay"));
    }
}
