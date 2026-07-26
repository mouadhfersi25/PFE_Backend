package com.britechnology.edugame.dto.voice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerOralHistorySessionDTO {
    private Long sessionId;
    private Long seriesId;
    private String seriesTitle;
    private Integer scoreFinal;
    private Integer accuracyPercent;
    private Integer xpGained;
    private Integer promptsReussis;
    private Integer promptsTotal;
    private Integer durationSeconds;
    private LocalDateTime dateFin;
}
