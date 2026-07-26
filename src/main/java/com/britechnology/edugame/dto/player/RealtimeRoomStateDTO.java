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
public class RealtimeRoomStateDTO {
    private String roomCode;
    private Long gameId;
    private int maxPlayers;
    private long createdAt;
    private Long startedAt;
    private boolean allReady;
    private List<RealtimeRoomPlayerDTO> players;
}
