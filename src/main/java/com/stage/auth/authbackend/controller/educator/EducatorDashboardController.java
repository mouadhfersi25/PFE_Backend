package com.stage.auth.authbackend.controller.educator;

import com.stage.auth.authbackend.dto.educator.EducatorDashboardStatsDTO;
import com.stage.auth.authbackend.service.educator.EducatorDashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * API dashboard éducateur. Synchronisé frontend educatorApi.getDashboardStats().
 * GET /api/educator/dashboard/stats -> EducatorDashboardStatsDTO (totalQuestionsCreated, assignedGames, avgSuccessRate, studentActivity, difficultyDistribution)
 */
@RestController
@RequestMapping("/api/educator/dashboard")
@RequiredArgsConstructor
public class EducatorDashboardController {

    private final EducatorDashboardService educatorDashboardService;

    @GetMapping("/stats")
    public ResponseEntity<EducatorDashboardStatsDTO> getStats() {
        return ResponseEntity.ok(educatorDashboardService.getStats());
    }
}
