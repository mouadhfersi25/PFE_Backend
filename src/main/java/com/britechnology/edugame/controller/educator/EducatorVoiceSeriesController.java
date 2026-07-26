package com.britechnology.edugame.controller.educator;

import com.britechnology.edugame.dto.voice.CreateVoiceSeriesRequest;
import com.britechnology.edugame.dto.voice.UpdateVoiceSeriesRequest;
import com.britechnology.edugame.dto.voice.VoiceSeriesDTO;
import com.britechnology.edugame.service.voice.EducatorVoiceSeriesService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/educator/voice/series")
@RequiredArgsConstructor
public class EducatorVoiceSeriesController {

    private final EducatorVoiceSeriesService educatorVoiceSeriesService;

    @GetMapping
    public ResponseEntity<List<VoiceSeriesDTO>> list(Authentication authentication) {
        return ResponseEntity.ok(educatorVoiceSeriesService.listByEducator(authentication.getName()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<VoiceSeriesDTO> getById(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(educatorVoiceSeriesService.findById(id, authentication.getName()));
    }

    @PostMapping
    public ResponseEntity<VoiceSeriesDTO> create(
            @Valid @RequestBody CreateVoiceSeriesRequest request,
            Authentication authentication
    ) {
        VoiceSeriesDTO created = educatorVoiceSeriesService.create(request, authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<VoiceSeriesDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateVoiceSeriesRequest request,
            Authentication authentication
    ) {
        return ResponseEntity.ok(educatorVoiceSeriesService.update(id, request, authentication.getName()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id, Authentication authentication) {
        educatorVoiceSeriesService.delete(id, authentication.getName());
        return ResponseEntity.noContent().build();
    }

    @PatchMapping("/{id}/publish")
    public ResponseEntity<VoiceSeriesDTO> publish(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(educatorVoiceSeriesService.publish(id, authentication.getName()));
    }

    @PatchMapping("/{id}/archive")
    public ResponseEntity<VoiceSeriesDTO> archive(@PathVariable Long id, Authentication authentication) {
        return ResponseEntity.ok(educatorVoiceSeriesService.archive(id, authentication.getName()));
    }
}
