package com.britechnology.edugame.dto.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitiveRoomPlayerResultDTO {
    private Long playerId;
    private String playerName;
    private boolean submitted;
    private Integer score;
    /** PENDING, WINNER, LOSER ou DRAW. */
    private String outcome;
}
