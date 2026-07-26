package com.britechnology.edugame.dto.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CompetitiveRoomResultDTO {
    private String roomCode;
    private Long gameId;
    private int expectedPlayers;
    private int completedPlayers;
    private Integer highestScore;
    private boolean complete;
    private List<CompetitiveRoomPlayerResultDTO> players;
}
