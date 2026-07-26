package com.britechnology.edugame.service.player;

import com.britechnology.edugame.entity.ModeJeu;
import org.springframework.stereotype.Service;

@Service
public class SessionLaunchModeValidatorService {

    public void validate(ModeJeu gameMode, String requestedModeRaw, String roomCodeRaw) {
        ModeJeu requestedMode = parseRequestedMode(requestedModeRaw);
        String roomCode = roomCodeRaw != null ? roomCodeRaw.trim() : "";

        if (gameMode == ModeJeu.INDIVIDUEL) {
            if (requestedMode == ModeJeu.EN_LIGNE) {
                throw new IllegalArgumentException("Ce jeu est uniquement disponible en solo hors ligne");
            }
            if (!roomCode.isBlank()) {
                throw new IllegalArgumentException("Ce jeu est INDIVIDUEL: roomCode ne doit pas être fourni");
            }
            return;
        }

        if (requestedMode != ModeJeu.EN_LIGNE) {
            throw new IllegalArgumentException("Ce jeu doit être lancé dans une room en ligne");
        }
        if (roomCode.isBlank()) {
            throw new IllegalArgumentException("Ce jeu en ligne exige un roomCode");
        }
    }

    private ModeJeu parseRequestedMode(String raw) {
        if (raw == null || raw.isBlank()) return null;
        String value = raw.trim().toUpperCase();
        if ("INDIVIDUAL".equals(value) || "INDIVIDUEL".equals(value)) return ModeJeu.INDIVIDUEL;
        if ("ONLINE".equals(value) || "EN_LIGNE".equals(value)) return ModeJeu.EN_LIGNE;
        return null;
    }
}
