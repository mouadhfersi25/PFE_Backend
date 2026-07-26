package com.stage.auth.authbackend.dto.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SoloLeaderboardEntryDTO {
    private Long userId;
    private String displayName;
    private String avatarUrl;
    private Integer level;
    private Integer totalScore;
}
