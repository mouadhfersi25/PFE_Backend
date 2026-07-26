package com.stage.auth.authbackend.dto.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerProgressOverviewDTO {
    private Integer currentLevel;
    private Integer avgSuccessRate;
    private Integer totalSessions;
    private Integer weeklyPlaytimeMinutes;
    private Integer skillMath;
    private Integer skillLogic;
    private Integer skillMemory;
    private Integer skillReflex;
    private List<PlayerProgressPointDTO> progressData;
    private List<PlayerGameTypePerformanceDTO> performanceByGameType;
}
