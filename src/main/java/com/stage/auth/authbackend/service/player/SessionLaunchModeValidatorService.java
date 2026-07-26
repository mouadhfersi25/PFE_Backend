package com.stage.auth.authbackend.service.player;

import com.stage.auth.authbackend.entity.ModeJeu;
import org.springframework.stereotype.Service;

@Service
public class SessionLaunchModeValidatorService {

    public void validate(ModeJeu gameMode, String requestedModeRaw, String roomCodeRaw) {
        ModeJeu requestedMode = parseRequestedMode(requestedModeRaw);
        String roomCode = roomCodeRaw != null ? roomCodeRaw.trim() : "";

        if (gameMode == ModeJeu.INDIVIDUEL) {
            if (requestedMode == ModeJeu.COLLECTIF) {
                throw new IllegalArgumentException("Ce jeu est INDIVIDUEL: lancement collectif interdit");
            }
            if (!roomCode.isBlank()) {
                throw new IllegalArgumentException("Ce jeu est INDIVIDUEL: roomCode ne doit pas être fourni");
            }
            return;
        }

        if (requestedMode == ModeJeu.INDIVIDUEL) {
            throw new IllegalArgumentException("Ce jeu est COLLECTIF: lancement individuel interdit");
        }
        if (roomCode.isBlank()) {
            throw new IllegalArgumentException("Ce jeu est COLLECTIF: roomCode est requis");
        }
    }

    private ModeJeu parseRequestedMode(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim().toUpperCase();
        if ("INDIVIDUAL".equals(value) || "INDIVIDUEL".equals(value)) return ModeJeu.INDIVIDUEL;
        if ("COLLECTIVE".equals(value) || "COLLECTIF".equals(value)) return ModeJeu.COLLECTIF;
        return null;
    }
}
