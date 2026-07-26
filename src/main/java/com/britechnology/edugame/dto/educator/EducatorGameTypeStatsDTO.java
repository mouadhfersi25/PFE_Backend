package com.britechnology.edugame.dto.educator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EducatorGameTypeStatsDTO {
    private String gameType;
    private String label;
    private long sessions;
    private int avgSuccessRate;
    private String color;
}
