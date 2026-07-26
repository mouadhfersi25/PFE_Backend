package com.britechnology.edugame.controller.admin;

import com.britechnology.edugame.dto.reclamation.ReclamationDTO;
import com.britechnology.edugame.dto.reclamation.UpdateReclamationRequest;
import com.britechnology.edugame.service.reclamation.ReclamationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin/reclamations")
@RequiredArgsConstructor
public class AdminReclamationController {

    private final ReclamationService reclamationService;

    @GetMapping
    public ResponseEntity<List<ReclamationDTO>> list(
            @RequestParam(required = false) Boolean pending
    ) {
        return ResponseEntity.ok(reclamationService.listForAdmin(pending));
    }

    @GetMapping("/pending-count")
    public ResponseEntity<Map<String, Long>> pendingCount() {
        return ResponseEntity.ok(Map.of("count", reclamationService.countPending()));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ReclamationDTO> update(
            Authentication authentication,
            @PathVariable Long id,
            @RequestBody(required = false) UpdateReclamationRequest request
    ) {
        return ResponseEntity.ok(reclamationService.updateByAdmin(authentication, id, request));
    }
}
