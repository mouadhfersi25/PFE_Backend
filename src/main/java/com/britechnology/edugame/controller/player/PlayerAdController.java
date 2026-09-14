package com.britechnology.edugame.controller.player;

import com.britechnology.edugame.dto.player.PlayerAdDTO;
import com.britechnology.edugame.dto.player.RecordAdInteractionRequest;
import com.britechnology.edugame.service.sponsor.SponsorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/player/ads")
@RequiredArgsConstructor
public class PlayerAdController {

    private final SponsorService sponsorService;

    @GetMapping("/active")
    public ResponseEntity<PlayerAdDTO> getActiveAd(
            Authentication authentication,
            @RequestParam("jeuId") Long jeuId
    ) {
        PlayerAdDTO ad = sponsorService.getActiveAdForGame(authentication, jeuId);
        return ad == null ? ResponseEntity.noContent().build() : ResponseEntity.ok(ad);
    }

    @PostMapping("/{id}/interactions")
    public ResponseEntity<Void> recordInteraction(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody RecordAdInteractionRequest request
    ) {
        sponsorService.recordAdInteraction(authentication, id, request);
        return ResponseEntity.noContent().build();
    }
}
