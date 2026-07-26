package com.britechnology.edugame.service.player;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.britechnology.edugame.dto.player.CreateGameSessionRequest;
import com.britechnology.edugame.entity.SessionJeu;
import com.britechnology.edugame.entity.TypeJeu;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SmartAdjustmentServiceTest {

    @Mock
    private GroqClientService groqClientService;

    @Test
    void shouldFallbackWhenGroqDisabled() {
        when(groqClientService.isReady()).thenReturn(false);
        SmartAdjustmentService service = new SmartAdjustmentService(groqClientService, new ObjectMapper());

        SessionJeu recent = SessionJeu.builder().scoreFinal(200).build();
        SmartAdjustmentService.AdjustmentResult result = service.computeBoundedAdjustment(
                1L,
                TypeJeu.QUIZ,
                5,
                350,
                CreateGameSessionRequest.builder().accuracyPercent(90).build(),
                List.of(recent)
        );

        assertTrue(result.adjustment() <= 0.08);
        assertTrue(result.adjustment() >= -0.08);
        assertTrue(result.source().startsWith("fallback"));
    }
}
