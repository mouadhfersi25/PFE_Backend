package com.britechnology.edugame.dto.admin;

import com.britechnology.edugame.entity.TypeJeu;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminScoringDistributionDTO {
    private Long gameId;
    private String gameTitle;
    private TypeJeu gameType;
    private Long sessions;
    private Double avgScore;
    private Integer minScore;
    private Integer maxScore;
    private Double avgXp;
    private Long totalXp;
    private Double avgAdjustmentDelta;
    private Long anomalySessions;
}
