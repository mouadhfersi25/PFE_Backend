package com.britechnology.edugame.service.player;

import com.britechnology.edugame.dto.player.CreateGameSessionRequest;
import com.britechnology.edugame.entity.EtatSession;
import com.britechnology.edugame.entity.TypeJeu;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class ProgressionGuardrailService {

    public GuardrailResult apply(
            TypeJeu typeJeu,
            EtatSession etatSession,
            CreateGameSessionRequest request,
            int xpInput,
            long sameGameRecentSessions
    ) {
        List<String> flags = new ArrayList<>();
        int xp = Math.max(0, xpInput);
        boolean allowLevelUp = true;

        if (etatSession == EtatSession.ABANDONNE || etatSession == EtatSession.EN_COURS) {
            xp = 0;
            allowLevelUp = false;
            flags.add("no_level_up_non_terminated");
            return new GuardrailResult(xp, allowLevelUp, String.join(",", flags));
        }

        double quality = qualityScore(typeJeu, request);
        if (quality < minQualityThreshold(typeJeu)) {
            xp = (int) Math.round(xp * 0.4);
            allowLevelUp = false;
            flags.add("quality_gate_failed");
        }

        double antiFarmMultiplier = antiFarmMultiplier(sameGameRecentSessions);
        if (antiFarmMultiplier < 1.0) flags.add("anti_farm_decay");
        xp = (int) Math.round(xp * antiFarmMultiplier);

        if (!Boolean.TRUE.equals(request.getReussite())) {
            xp = (int) Math.round(xp * 0.7);
            flags.add("session_not_successful");
        }

        xp = Math.max(0, Math.min(60, xp));
        return new GuardrailResult(xp, allowLevelUp, String.join(",", flags));
    }

    private double qualityScore(TypeJeu typeJeu, CreateGameSessionRequest request) {
        return switch (typeJeu) {
            case QUIZ -> quizQuality(request);
            case REFLEXE -> reflexQuality(request);
            case MEMOIRE -> memoryQuality(request);
            case LOGIQUE -> logicQuality(request);
        };
    }

    private double quizQuality(CreateGameSessionRequest request) {
        if (request.getAccuracyPercent() != null) return normalizePercent(request.getAccuracyPercent());
        if (request.getTotalQuestions() != null && request.getTotalQuestions() > 0 && request.getCorrectAnswers() != null) {
            return (double) request.getCorrectAnswers() / request.getTotalQuestions();
        }
        return 0.0;
    }

    private double reflexQuality(CreateGameSessionRequest request) {
        if (request.getTotalRounds() != null && request.getTotalRounds() > 0 && request.getSuccessfulRounds() != null) {
            return (double) request.getSuccessfulRounds() / request.getTotalRounds();
        }
        return 0.0;
    }

    private double memoryQuality(CreateGameSessionRequest request) {
        if (request.getMoves() != null && request.getMoves() > 0 && request.getMatches() != null) {
            return (double) request.getMatches() / request.getMoves();
        }
        return Boolean.TRUE.equals(request.getReussite()) ? 0.7 : 0.2;
    }

    private double logicQuality(CreateGameSessionRequest request) {
        int attempts = request.getAttempts() == null ? 1 : Math.max(1, request.getAttempts());
        int hints = request.getHintsUsed() == null ? 0 : Math.max(0, request.getHintsUsed());
        double attemptsScore = Math.max(0.0, 1.0 - ((attempts - 1) * 0.18));
        double hintsPenalty = Math.min(0.6, hints * 0.15);
        return Math.max(0.0, attemptsScore - hintsPenalty);
    }

    private double normalizePercent(Integer percent) {
        return Math.max(0.0, Math.min(1.0, percent / 100.0));
    }

    private double minQualityThreshold(TypeJeu typeJeu) {
        return switch (typeJeu) {
            case QUIZ -> 0.55;
            case REFLEXE -> 0.5;
            case MEMOIRE -> 0.4;
            case LOGIQUE -> 0.45;
        };
    }

    private double antiFarmMultiplier(long sameGameRecentSessions) {
        if (sameGameRecentSessions >= 6) return 0.35;
        if (sameGameRecentSessions >= 4) return 0.55;
        if (sameGameRecentSessions >= 2) return 0.75;
        return 1.0;
    }

    public record GuardrailResult(int xpAfterGuardrails, boolean allowLevelUp, String guardrailNotes) {}
}
