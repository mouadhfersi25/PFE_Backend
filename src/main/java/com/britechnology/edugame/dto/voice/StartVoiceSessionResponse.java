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
public class StartVoiceSessionResponse {
    private Long sessionOralId;
    private Long seriesId;
    private String seriesTitle;
    private int promptsTotal;
    private LocalDateTime dateDebut;
}
