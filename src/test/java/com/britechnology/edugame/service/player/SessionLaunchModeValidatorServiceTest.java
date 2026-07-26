package com.britechnology.edugame.service.player;

import com.britechnology.edugame.entity.ModeJeu;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

class SessionLaunchModeValidatorServiceTest {

    private final SessionLaunchModeValidatorService validator = new SessionLaunchModeValidatorService();

    @Test
    void shouldAllowIndividualWithoutRoomCode() {
        assertDoesNotThrow(() -> validator.validate(ModeJeu.INDIVIDUEL, "INDIVIDUEL", null));
    }

    @Test
    void shouldRejectOnlineLaunchOnIndividualGame() {
        assertThrows(IllegalArgumentException.class, () ->
                validator.validate(ModeJeu.INDIVIDUEL, "EN_LIGNE", "ABC123"));
    }

    @Test
    void shouldAllowOnlineWithRoomCode() {
        assertDoesNotThrow(() -> validator.validate(ModeJeu.EN_LIGNE, "EN_LIGNE", "ABC123"));
    }

    @Test
    void shouldRejectOnlineWithoutRoomCode() {
        assertThrows(IllegalArgumentException.class, () ->
                validator.validate(ModeJeu.EN_LIGNE, "EN_LIGNE", null));
    }
}
