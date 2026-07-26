package com.britechnology.edugame.dto.voice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompleteVoiceSessionResponse {
    private Long sessionOralId;
    private Integer scoreFinal;
    private Integer scoreBase;
    private Integer xpGained;
    private Integer accuracyPercent;
    private Integer promptsReussis;
    private Integer promptsTotal;
    private Integer totalScore;
    private Integer previousLevel;
    private Integer newLevel;
    private Integer previousXp;
    private Integer newXp;
    private Integer xpToNextLevel;
    private Boolean levelUp;
    private LocalDateTime dateFin;
    private Integer durationSeconds;
}
