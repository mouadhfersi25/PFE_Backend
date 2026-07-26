package com.stage.auth.authbackend.dto.player;

import lombok.Data;

@Data
public class CreateRoomRequest {
    private Long gameId;
    private String teamName;
}
