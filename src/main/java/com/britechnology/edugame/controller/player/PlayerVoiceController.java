package com.britechnology.edugame.controller.player;

import com.britechnology.edugame.dto.voice.*;
import com.britechnology.edugame.service.voice.PlayerVoiceService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/player/voice")
@RequiredArgsConstructor
public class PlayerVoiceController {

    private final PlayerVoiceService playerVoiceService;

    @GetMapping("/series")
    public ResponseEntity<List<VoiceSeriesDTO>> listPublishedSeries() {
        return ResponseEntity.ok(playerVoiceService.listPublishedSeries());
    }

    @GetMapping("/series/{id}")
    public ResponseEntity<VoiceSeriesDTO> getPublishedSeries(@PathVariable Long id) {
        return ResponseEntity.ok(playerVoiceService.getPublishedSeries(id));
    }

    @PostMapping("/sessions/start")
    public ResponseEntity<StartVoiceSessionResponse> startSession(
            @RequestParam Long seriesId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(playerVoiceService.startSession(seriesId, authentication.getName()));
    }

    @PostMapping(value = "/evaluate", consumes = "multipart/form-data")
    public ResponseEntity<VoiceEvaluationResultDTO> evaluate(
            @RequestParam Long promptId,
            @RequestParam Long sessionOralId,
            @RequestParam(required = false) Integer dureeSecondes,
            @RequestParam(required = false) String browserTranscript,
            @RequestPart(value = "audio", required = false) MultipartFile audio,
            Authentication authentication
    ) {
        return ResponseEntity.ok(playerVoiceService.evaluate(
                promptId,
                sessionOralId,
                audio,
                dureeSecondes,
                browserTranscript,
                authentication.getName()
        ));
    }

    @PostMapping("/sessions/{sessionOralId}/complete")
    public ResponseEntity<CompleteVoiceSessionResponse> completeSession(
            @PathVariable Long sessionOralId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(playerVoiceService.completeSession(sessionOralId, authentication.getName()));
    }

    @GetMapping("/history")
    public ResponseEntity<List<PlayerOralHistorySessionDTO>> history(Authentication authentication) {
        return ResponseEntity.ok(playerVoiceService.getHistory(authentication.getName()));
    }
}
