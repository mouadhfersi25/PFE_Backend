package com.britechnology.edugame.controller.sponsor;

import com.britechnology.edugame.dto.sponsor.*;
import com.britechnology.edugame.service.sponsor.SponsorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/sponsor")
@RequiredArgsConstructor
public class SponsorController {

    private final SponsorService sponsorService;

    @GetMapping("/dashboard/stats")
    public ResponseEntity<SponsorDashboardStatsDTO> getDashboardStats(Authentication authentication) {
        return ResponseEntity.ok(sponsorService.getDashboardStats(authentication));
    }

    @GetMapping("/publicites")
    public ResponseEntity<List<PubliciteDTO>> listPublicites(Authentication authentication) {
        return ResponseEntity.ok(sponsorService.listPublicites(authentication));
    }

    @PostMapping("/publicites")
    public ResponseEntity<PubliciteDTO> createPublicite(
            Authentication authentication,
            @RequestBody(required = false) Map<String, Object> request
    ) {
        return ResponseEntity.ok(sponsorService.createPublicite(authentication, request));
    }

    @PutMapping("/publicites/{id}")
    public ResponseEntity<PubliciteDTO> updatePublicite(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody(required = false) Map<String, Object> request
    ) {
        return ResponseEntity.ok(sponsorService.updatePublicite(authentication, id, request));
    }

    @PatchMapping("/publicites/{id}/status")
    public ResponseEntity<PubliciteDTO> setPubliciteStatus(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam("active") boolean active
    ) {
        return ResponseEntity.ok(sponsorService.setPubliciteStatus(authentication, id, active));
    }

    @DeleteMapping("/publicites/{id}")
    public ResponseEntity<Void> deletePublicite(
            Authentication authentication,
            @PathVariable Long id
    ) {
        sponsorService.deletePublicite(authentication, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/recompenses")
    public ResponseEntity<List<RecompenseDTO>> listRecompenses(Authentication authentication) {
        return ResponseEntity.ok(sponsorService.listRecompenses(authentication));
    }

    @GetMapping("/recompenses/{id}")
    public ResponseEntity<RecompenseDTO> getRecompenseById(
            Authentication authentication,
            @PathVariable Long id
    ) {
        return ResponseEntity.ok(sponsorService.getRecompenseById(authentication, id));
    }

    @PostMapping("/recompenses")
    public ResponseEntity<RecompenseDTO> createRecompense(
            Authentication authentication,
            @RequestBody CreateRecompenseRequest request
    ) {
        return ResponseEntity.ok(sponsorService.createRecompense(authentication, request));
    }

    @PutMapping("/recompenses/{id}")
    public ResponseEntity<RecompenseDTO> updateRecompense(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody UpdateRecompenseRequest request
    ) {
        return ResponseEntity.ok(sponsorService.updateRecompense(authentication, id, request));
    }

    @PatchMapping("/recompenses/{id}/status")
    public ResponseEntity<RecompenseDTO> setRecompenseStatus(
            Authentication authentication,
            @PathVariable Long id,
            @RequestParam("active") boolean active
    ) {
        return ResponseEntity.ok(sponsorService.setRecompenseStatus(authentication, id, active));
    }

    @DeleteMapping("/recompenses/{id}")
    public ResponseEntity<Void> deleteRecompense(
            Authentication authentication,
            @PathVariable Long id
    ) {
        sponsorService.deleteRecompense(authentication, id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/reward-requests")
    public ResponseEntity<List<SponsorRewardRequestDTO>> listRewardRequests(Authentication authentication) {
        return ResponseEntity.ok(sponsorService.listRewardRequests(authentication));
    }

    @PatchMapping("/reward-requests/{requestId}/status")
    public ResponseEntity<SponsorRewardRequestDTO> updateRewardRequestStatus(
            Authentication authentication,
            @PathVariable Long requestId,
            @RequestParam("status") String status
    ) {
        return ResponseEntity.ok(sponsorService.updateRewardRequestStatus(authentication, requestId, status));
    }
}
