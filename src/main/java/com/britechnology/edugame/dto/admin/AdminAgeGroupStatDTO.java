package com.britechnology.edugame.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminAgeGroupStatDTO {
    /** Tranche d'âge (ex. 0-7, 8-12, 13-17, 18+). */
    private String age;
    private double avgScore;
    private long players;
}
