package com.britechnology.edugame.controller.educator;

import com.britechnology.edugame.dto.educator.CreateQuizQuestionRequest;
import com.britechnology.edugame.dto.educator.GenerateQuizPreviewRequest;
import com.britechnology.edugame.dto.educator.QuizQuestionDTO;
import com.britechnology.edugame.dto.educator.UpdateQuizQuestionRequest;
import com.britechnology.edugame.service.educator.AiQuizGenerationService;
import com.britechnology.edugame.service.educator.EducatorQuestionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * API éducateur : questions quiz. Synchronisé frontend educatorApi (getQuestions, getQuestionById, createQuestion, updateQuestion, deleteQuestion).
 * GET /api/educator/questions?gameId= -> QuizQuestionDTO[]
 * GET /api/educator/questions/{id} -> QuizQuestionDTO
 * POST /api/educator/questions -> 201 QuizQuestionDTO
 * PUT /api/educator/questions/{id} -> QuizQuestionDTO
 * DELETE /api/educator/questions/{id} -> 204
 */
@RestController
@RequestMapping("/api/educator/questions")
@RequiredArgsConstructor
public class EducatorQuestionController {

    private final EducatorQuestionService educatorQuestionService;
    private final AiQuizGenerationService aiQuizGenerationService;

    @GetMapping
    public ResponseEntity<List<QuizQuestionDTO>> listByGame(@RequestParam Long gameId) {
        return ResponseEntity.ok(educatorQuestionService.listByGame(gameId));
    }

    @GetMapping("/{id}")
    public ResponseEntity<QuizQuestionDTO> getById(@PathVariable Long id) {
        return ResponseEntity.ok(educatorQuestionService.findById(id));
    }

    @PostMapping
    public ResponseEntity<QuizQuestionDTO> create(@Valid @RequestBody CreateQuizQuestionRequest request) {
        QuizQuestionDTO dto = educatorQuestionService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @PutMapping("/{id}")
    public ResponseEntity<QuizQuestionDTO> update(
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuizQuestionRequest request
    ) {
        return ResponseEntity.ok(educatorQuestionService.update(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        educatorQuestionService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/ai/ping")
    public ResponseEntity<Map<String, Object>> pingAi() {
        return ResponseEntity.ok(aiQuizGenerationService.ping());
    }

    @PostMapping("/ai/generate-preview")
    public ResponseEntity<List<QuizQuestionDTO>> generatePreview(@Valid @RequestBody GenerateQuizPreviewRequest request) {
        return ResponseEntity.ok(aiQuizGenerationService.generatePreview(
                request.getGameId(),
                request.getCount(),
                request.getSousType()
        ));
    }

    @PostMapping("/{id}/media/upload")
    public ResponseEntity<QuizQuestionDTO> uploadMedia(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(educatorQuestionService.uploadMedia(id, file));
    }

    @PostMapping("/{id}/prompt-audio/upload")
    public ResponseEntity<QuizQuestionDTO> uploadPromptAudio(
            @PathVariable Long id,
            @RequestParam("file") MultipartFile file
    ) {
        return ResponseEntity.ok(educatorQuestionService.uploadPromptAudio(id, file));
    }
}
