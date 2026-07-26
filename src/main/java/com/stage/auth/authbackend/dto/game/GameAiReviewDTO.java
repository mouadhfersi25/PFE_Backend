package com.stage.auth.authbackend.dto.game;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GameAiReviewDTO {
    private Long gameId;
    private String gameTitle;
    private String model;
    private Integer score;
    private String riskLevel;
    private String suggestedAction;
    private String summary;
    private List<String> strengths;
    private List<String> issues;
    private List<String> recommendations;
}
