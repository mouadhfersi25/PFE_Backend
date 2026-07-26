package com.stage.auth.authbackend.service.player;

import com.stage.auth.authbackend.entity.ModeJeu;
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
    void shouldRejectCollectiveLaunchOnIndividualGame() {
        assertThrows(IllegalArgumentException.class, () ->
                validator.validate(ModeJeu.INDIVIDUEL, "COLLECTIF", "ABC123"));
    }

    @Test
    void shouldAllowCollectiveWithRoomCode() {
        assertDoesNotThrow(() -> validator.validate(ModeJeu.COLLECTIF, "COLLECTIF", "ABC123"));
    }

    @Test
    void shouldRejectCollectiveWithoutRoomCode() {
        assertThrows(IllegalArgumentException.class, () ->
                validator.validate(ModeJeu.COLLECTIF, "COLLECTIF", null));
    }
}
