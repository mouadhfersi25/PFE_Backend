package com.stage.auth.authbackend.controller.admin;

import com.stage.auth.authbackend.dto.badge.BadgeDTO;
import com.stage.auth.authbackend.dto.badge.CreateBadgeRequest;
import com.stage.auth.authbackend.dto.badge.UpdateBadgeRequest;
import com.stage.auth.authbackend.service.admin.AdminBadgeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API admin pour la gestion des badges (Manage Badges).
 * Toutes les routes nécessitent le rôle ADMIN.
 */
@RestController
@RequestMapping("/api/admin/badges")
@RequiredArgsConstructor
public class AdminBadgeController {

    private final AdminBadgeService adminBadgeService;

    @GetMapping
    public ResponseEntity<List<BadgeDTO>> getAllBadges() {
        return ResponseEntity.ok(adminBadgeService.findAllBadges());
    }

    @GetMapping("/{id}")
    public ResponseEntity<BadgeDTO> getBadgeById(@PathVariable Long id) {
        return ResponseEntity.ok(adminBadgeService.findBadgeById(id));
    }

    @PostMapping
    public ResponseEntity<BadgeDTO> createBadge(@Valid @RequestBody CreateBadgeRequest request) {
        BadgeDTO badge = adminBadgeService.createBadge(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(badge);
    }

    @PutMapping("/{id}")
    public ResponseEntity<BadgeDTO> updateBadge(@PathVariable Long id, @Valid @RequestBody UpdateBadgeRequest request) {
        return ResponseEntity.ok(adminBadgeService.updateBadge(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteBadge(@PathVariable Long id) {
        adminBadgeService.deleteBadge(id);
        return ResponseEntity.noContent().build();
    }
}
