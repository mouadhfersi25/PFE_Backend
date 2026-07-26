package com.britechnology.edugame.dto.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateGameSessionResponse {
    private Long sessionId;
    private Integer scoreGlobal;
    private Integer scoreBase;
    private Integer scoreFinal;
    /** Score théorique maximum pour ce type de jeu (affichage score/max). */
    private Integer scoreMaxPossible;
    private Double aiAdjustment;
    private Integer totalScore;
    private Integer xpGained;
    private Integer previousLevel;
    private Integer newLevel;
    private Integer previousXp;
    private Integer newXp;
    private Integer xpToNextLevel;
    private Integer durationSeconds;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private boolean levelUp;
    private String scoringRulesVersion;
    private String anomalyNotes;
    private String adjustmentSource;
    private String explanationCode;
    /** Présent uniquement pour une partie compétitive EN_LIGNE. */
    private CompetitiveRoomResultDTO roomResult;
    private String message;
}
