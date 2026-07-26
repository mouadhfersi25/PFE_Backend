package com.britechnology.edugame.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminStatisticsOverviewDTO {
    private List<AdminGamePerformanceDTO> gamePerformance;
    private List<AdminAgeGroupStatDTO> ageGroups;
    private AdminGlobalMetricsDTO metrics;
}
