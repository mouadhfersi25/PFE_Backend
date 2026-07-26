package com.stage.auth.authbackend.controller.player;

import com.stage.auth.authbackend.dto.player.RoomReadyMessage;
import com.stage.auth.authbackend.dto.player.RoomTeamNameMessage;
import com.stage.auth.authbackend.service.player.RealtimeRoomService;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.stereotype.Controller;

import java.security.Principal;

@Controller
@RequiredArgsConstructor
public class PlayerRoomSocketController {

    private final RealtimeRoomService realtimeRoomService;

    @MessageMapping("/rooms/{roomCode}/ready")
    public void updateReady(
            @DestinationVariable String roomCode,
            RoomReadyMessage message,
            Principal principal
    ) {
        boolean ready = message != null && Boolean.TRUE.equals(message.getReady());
        realtimeRoomService.setReady(principal.getName(), roomCode, ready);
    }

    @MessageMapping("/rooms/{roomCode}/start")
    public void startRoom(
            @DestinationVariable String roomCode,
            Principal principal
    ) {
        realtimeRoomService.startRoom(principal.getName(), roomCode);
    }

    @MessageMapping("/rooms/{roomCode}/team-name")
    public void updateTeamName(
            @DestinationVariable String roomCode,
            RoomTeamNameMessage message,
            Principal principal
    ) {
        String teamName = message != null ? message.getTeamName() : null;
        realtimeRoomService.updateTeamName(principal.getName(), roomCode, teamName);
    }
}
