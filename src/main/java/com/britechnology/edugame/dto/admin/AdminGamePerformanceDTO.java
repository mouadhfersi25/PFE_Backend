package com.britechnology.edugame.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminGamePerformanceDTO {
    private String name;
    private Long plays;
    private double avgScore;
    /** Moyenne du pourcentage de précision (0–100) sur les sessions terminées. */
    private double completion;
}
