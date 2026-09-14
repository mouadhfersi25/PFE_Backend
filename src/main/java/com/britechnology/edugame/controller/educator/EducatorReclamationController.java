package com.britechnology.edugame.controller.educator;

import com.britechnology.edugame.dto.reclamation.ReclamationDTO;
import com.britechnology.edugame.service.reclamation.ReclamationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * API éducateur : signalements joueurs validés concernant ses propres jeux.
 * GET /api/educator/reclamations -> ReclamationDTO[] (statut TRAITE uniquement)
 */
@RestController
@RequestMapping("/api/educator/reclamations")
@RequiredArgsConstructor
public class EducatorReclamationController {

    private final ReclamationService reclamationService;

    @GetMapping
    public ResponseEntity<List<ReclamationDTO>> list(Authentication authentication) {
        return ResponseEntity.ok(reclamationService.listForEducator(authentication));
    }
}
