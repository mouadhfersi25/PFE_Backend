package com.stage.auth.authbackend.dto.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerGameTypePerformanceDTO {
    private String type;
    private Integer played;
    private Integer avgScore;
    private Integer successRate;
}
