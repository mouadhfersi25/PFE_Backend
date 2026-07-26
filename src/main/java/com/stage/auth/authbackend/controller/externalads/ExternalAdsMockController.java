package com.stage.auth.authbackend.controller.externalads;

import com.stage.auth.authbackend.service.externalads.ExternalAdsMockService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/ext-ads")
@RequiredArgsConstructor
public class ExternalAdsMockController {

    private final ExternalAdsMockService mockService;

    @GetMapping("/stats")
    public ResponseEntity<?> stats(@RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        if (!mockService.isAuthorized(apiKey)) return unauthorized();
        return ResponseEntity.ok(mockService.stats());
    }

    @GetMapping("/rewards")
    public ResponseEntity<?> rewards(@RequestHeader(value = "X-API-Key", required = false) String apiKey) {
        if (!mockService.isAuthorized(apiKey)) return unauthorized();
        return ResponseEntity.ok(mockService.listRewards());
    }

    private ResponseEntity<Map<String, String>> unauthorized() {
        return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of("message", "invalid api key"));
    }
}
