package com.britechnology.edugame.controller.educator;

import com.britechnology.edugame.dto.educator.GenerateQuizPreviewRequest;
import com.britechnology.edugame.dto.educator.QuizQuestionDTO;
import com.britechnology.edugame.service.educator.AiQuizGenerationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/educator/ai")
@RequiredArgsConstructor
public class EducatorAiController {

    private final AiQuizGenerationService aiQuizGenerationService;

    @GetMapping("/ping")
    public ResponseEntity<Map<String, Object>> pingAi() {
        return ResponseEntity.ok(aiQuizGenerationService.ping());
    }

    @PostMapping("/generate-preview")
    public ResponseEntity<List<QuizQuestionDTO>> generatePreview(@Valid @RequestBody GenerateQuizPreviewRequest request) {
        return ResponseEntity.ok(aiQuizGenerationService.generatePreview(
                request.getGameId(),
                request.getCount(),
                request.getSousType()
        ));
    }
}
