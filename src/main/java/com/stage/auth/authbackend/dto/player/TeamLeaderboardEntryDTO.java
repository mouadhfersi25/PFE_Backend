package com.stage.auth.authbackend.dto.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TeamLeaderboardEntryDTO {
    private String teamName;
    private String roomCode;
    private Long sessionsCount;
    private Long playersCount;
    private Double averageScore;
    private Long totalScore;
}
