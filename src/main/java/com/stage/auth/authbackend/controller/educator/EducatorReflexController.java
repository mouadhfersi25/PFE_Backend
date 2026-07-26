package com.stage.auth.authbackend.controller.educator;

import com.stage.auth.authbackend.dto.educator.CreateOrUpdateReflexSettingsRequest;
import com.stage.auth.authbackend.dto.educator.GenerateReflexSettingsPreviewRequest;
import com.stage.auth.authbackend.dto.educator.ReflexSettingsDTO;
import com.stage.auth.authbackend.service.educator.AiReflexGenerationService;
import com.stage.auth.authbackend.service.educator.EducatorReflexService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * API éducateur : paramètres des jeux réflexe (jeux type REFLEXE).
 * Un seul jeu réflexe = un seul set de paramètres (create or update).
 * Rôle EDUCATEUR requis.
 */
@RestController
@RequestMapping("/api/educator/reflex-settings")
@RequiredArgsConstructor
public class EducatorReflexController {

    private final EducatorReflexService educatorReflexService;
    private final AiReflexGenerationService aiReflexGenerationService;

    @GetMapping
    public ResponseEntity<ReflexSettingsDTO> getByGame(@RequestParam Long gameId) {
        ReflexSettingsDTO dto = educatorReflexService.getByGame(gameId);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.noContent().build();
    }

    @PutMapping
    public ResponseEntity<ReflexSettingsDTO> createOrUpdate(@Valid @RequestBody CreateOrUpdateReflexSettingsRequest request) {
        ReflexSettingsDTO dto = educatorReflexService.createOrUpdate(request);
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/ai/generate-preview")
    public ResponseEntity<ReflexSettingsDTO> generatePreview(@Valid @RequestBody GenerateReflexSettingsPreviewRequest request) {
        return ResponseEntity.ok(aiReflexGenerationService.generatePreview(request.getGameId()));
    }
}
