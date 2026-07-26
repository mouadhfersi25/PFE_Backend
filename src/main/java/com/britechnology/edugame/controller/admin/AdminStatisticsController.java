package com.britechnology.edugame.controller.admin;

import com.britechnology.edugame.dto.admin.AdminDaySessionCountDTO;
import com.britechnology.edugame.dto.admin.AdminRecentActivityDTO;
import com.britechnology.edugame.dto.admin.AdminStatisticsOverviewDTO;
import com.britechnology.edugame.service.admin.AdminStatisticsService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/statistics")
@RequiredArgsConstructor
public class AdminStatisticsController {

    private final AdminStatisticsService adminStatisticsService;

    @GetMapping("/sessions-by-day")
    public ResponseEntity<List<AdminDaySessionCountDTO>> sessionsLast7Days() {
        return ResponseEntity.ok(adminStatisticsService.getSessionsLast7Days());
    }

    @GetMapping("/recent-activity")
    public ResponseEntity<List<AdminRecentActivityDTO>> recentActivity(
            @RequestParam(defaultValue = "20") int limit) {
        return ResponseEntity.ok(adminStatisticsService.getRecentActivity(limit));
    }

    @GetMapping("/overview")
    public ResponseEntity<AdminStatisticsOverviewDTO> overview() {
        return ResponseEntity.ok(adminStatisticsService.getOverview());
    }
}
