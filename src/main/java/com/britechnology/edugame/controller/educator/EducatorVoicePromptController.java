package com.britechnology.edugame.controller.educator;

import com.britechnology.edugame.dto.voice.CreateVoicePromptRequest;
import com.britechnology.edugame.dto.voice.UpdateVoicePromptRequest;
import com.britechnology.edugame.dto.voice.VoicePromptDTO;
import com.britechnology.edugame.service.voice.EducatorVoicePromptService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/educator/voice/prompts")
@RequiredArgsConstructor
public class EducatorVoicePromptController {

    private final EducatorVoicePromptService educatorVoicePromptService;

    @GetMapping
    public ResponseEntity<List<VoicePromptDTO>> listBySeries(
            @RequestParam Long seriesId,
            Authentication authentication
    ) {
        return ResponseEntity.ok(educatorVoicePromptService.listBySeries(seriesId, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<VoicePromptDTO> create(
            @Valid @RequestBody CreateVoicePromptRequest request,
            Authentication authentication
    ) {
        VoicePromptDTO created = educatorVoicePromptService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VoicePromptDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVoicePromptRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(educatorVoicePromptService.update(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        educatorVoicePromptService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }
}
