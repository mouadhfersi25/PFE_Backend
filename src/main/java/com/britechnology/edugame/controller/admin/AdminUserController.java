package com.britechnology.edugame.controller.admin;

import com.britechnology.edugame.dto.admin.AdminScoringDistributionDTO;
import com.britechnology.edugame.dto.player.SetParentLinkRequest;
import com.britechnology.edugame.dto.player.UpdateRoleRequest;
import com.britechnology.edugame.dto.player.UserDTO;
import com.britechnology.edugame.service.admin.AdminUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    /**
     * GET /api/admin/users
     * Liste tous les utilisateurs (réservé au rôle ADMIN).
     */
    @GetMapping
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        List<UserDTO> users = adminUserService.findAllUsers();
        return ResponseEntity.ok(users);
    }

    /**
     * GET /api/admin/users/{id}
     * Profil d'un utilisateur par id (réservé au rôle ADMIN). Retourne UserDTO (table users).
     */
    @GetMapping("/{id}")
    public ResponseEntity<UserDTO> getUserById(@PathVariable Long id) {
        UserDTO user = adminUserService.findUserById(id);
        return ResponseEntity.ok(user);
    }

    /**
     * POST /api/admin/users/suspend/{id}
     * Suspendre un utilisateur (état SUSPENDU, enabled = false).
     */
    @PostMapping("/suspend/{id}")
    public ResponseEntity<UserDTO> suspendUser(@PathVariable Long id) {
        UserDTO user = adminUserService.suspendUser(id);
        return ResponseEntity.ok(user);
    }

    /**
     * POST /api/admin/users/reactivate/{id}
     * Réactiver un utilisateur (état ACTIF, enabled = true).
     */
    @PostMapping("/reactivate/{id}")
    public ResponseEntity<UserDTO> reactivateUser(@PathVariable Long id) {
        UserDTO user = adminUserService.reactivateUser(id);
        return ResponseEntity.ok(user);
    }

    /**
     * PUT /api/admin/users/{id}/change-role
     * Changer le rôle d'un utilisateur (sauf son propre rôle et sauf le rôle ADMIN).
     */
    @PutMapping("/{id}/change-role")
    public ResponseEntity<UserDTO> updateUserRole(
            @PathVariable Long id,
            @RequestBody UpdateRoleRequest request,
            Authentication authentication) {
        String currentEmail = authentication != null ? authentication.getName() : null;
        UserDTO user = adminUserService.updateRole(id, request, currentEmail);
        return ResponseEntity.ok(user);
    }

    @PutMapping("/{id}/parent-link")
    public ResponseEntity<UserDTO> setParentLink(
            @PathVariable Long id,
            @RequestBody(required = false) SetParentLinkRequest request) {
        Long parentId = request != null ? request.getParentId() : null;
        return ResponseEntity.ok(adminUserService.setParentLink(id, parentId));
    }

    @GetMapping("/sessions/active-count")
    public ResponseEntity<Map<String, Long>> getActiveSessionsCount() {
        return ResponseEntity.ok(Map.of("activeSessions", adminUserService.getActiveSessionsCount()));
    }

    @GetMapping("/scoring-distribution")
    public ResponseEntity<List<AdminScoringDistributionDTO>> getScoringDistribution() {
        return ResponseEntity.ok(adminUserService.getScoringDistribution());
    }
}
