package com.britechnology.edugame.controller.player;

import com.britechnology.edugame.dto.player.RoomReadyMessage;
import com.britechnology.edugame.service.player.RealtimeRoomService;
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

}
