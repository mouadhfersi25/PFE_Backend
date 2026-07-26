package com.britechnology.edugame.dto.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGameSessionRequest {
    private Long gameId;
    private String modeJeu;
    private String roomCode;
    private Integer scoreGlobal;
    private Integer niveauAtteint;
    private String etatSession;
    private Integer durationSeconds;
    private Integer accuracyPercent;
    private Integer reactionTimeMs;
    private Integer totalQuestions;
    private Integer correctAnswers;
    private Integer moves;
    private Integer matches;
    private Integer attempts;
    private Integer hintsUsed;
    private Integer totalRounds;
    private Integer successfulRounds;
    private Boolean reussite;
}
