package com.stage.auth.authbackend.controller.educator;

import com.stage.auth.authbackend.dto.educator.CreateLogicPuzzleRequest;
import com.stage.auth.authbackend.dto.educator.LogicPuzzleDTO;
import com.stage.auth.authbackend.dto.educator.UpdateLogicPuzzleRequest;
import com.stage.auth.authbackend.service.educator.EducatorLogicService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API éducateur : gestion des puzzles logiques (jeux type LOGIQUE).
 * Rôle EDUCATEUR requis.
 */
@RestController
@RequestMapping("/api/educator/logic-puzzles")
@RequiredArgsConstructor
public class EducatorLogicController {

    private final EducatorLogicService educatorLogicService;

    @GetMapping
    public ResponseEntity<List<LogicPuzzleDTO>> listByGame(@RequestParam Long gameId) {
        return ResponseEntity.ok(educatorLogicService.listByGame(gameId));
    }

    @PostMapping
    public ResponseEntity<LogicPuzzleDTO> create(@Valid @RequestBody CreateLogicPuzzleRequest request) {
        LogicPuzzleDTO dto = educatorLogicService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<LogicPuzzleDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateLogicPuzzleRequest request
    ) {
        return ResponseEntity.ok(educatorLogicService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        educatorLogicService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
