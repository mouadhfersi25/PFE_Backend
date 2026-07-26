package com.britechnology.edugame.controller.educator;

import com.britechnology.edugame.dto.educator.EducatorLearningStatsDTO;
import com.britechnology.edugame.service.educator.EducatorLearningStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/educator/statistics")
@RequiredArgsConstructor
public class EducatorStatisticsController {

    private final EducatorLearningStatisticsService learningStatisticsService;

    @GetMapping("/learning")
    public ResponseEntity<EducatorLearningStatsDTO> getLearningStats() {
        return ResponseEntity.ok(learningStatisticsService.getLearningStats());
    }
}
