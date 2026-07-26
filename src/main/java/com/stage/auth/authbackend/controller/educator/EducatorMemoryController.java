package com.stage.auth.authbackend.controller.educator;

import com.stage.auth.authbackend.dto.educator.CreateMemoryCardRequest;
import com.stage.auth.authbackend.dto.educator.MemoryCardDTO;
import com.stage.auth.authbackend.dto.educator.UpdateMemoryCardRequest;
import com.stage.auth.authbackend.service.educator.EducatorMemoryService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API éducateur : cartes mémoire (paires). Synchronisé frontend educatorApi (getMemoryCards, createMemoryCard, updateMemoryCard, deleteMemoryCard).
 * GET /api/educator/memory-cards?gameId= -> MemoryCardDTO[]
 * POST /api/educator/memory-cards -> 201 MemoryCardDTO
 * PUT /api/educator/memory-cards/{id} -> MemoryCardDTO
 * DELETE /api/educator/memory-cards/{id} -> 204
 */
@RestController
@RequestMapping("/api/educator/memory-cards")
@RequiredArgsConstructor
public class EducatorMemoryController {

    private final EducatorMemoryService educatorMemoryService;

    @GetMapping
    public ResponseEntity<List<MemoryCardDTO>> listByGame(@RequestParam Long gameId) {
        return ResponseEntity.ok(educatorMemoryService.listByGame(gameId));
    }

    @PostMapping
    public ResponseEntity<MemoryCardDTO> create(@Valid @RequestBody CreateMemoryCardRequest request) {
        MemoryCardDTO dto = educatorMemoryService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<MemoryCardDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateMemoryCardRequest request
    ) {
        return ResponseEntity.ok(educatorMemoryService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        educatorMemoryService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
