package com.britechnology.edugame.dto.voice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceSeriesDTO {
    private Long id;
    private String titre;
    private String description;
    private String langue;
    private Integer difficulte;
    private String etat;
    private Long educateurId;
    private String educateurNom;
    private int promptsCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime publishedAt;
    private List<VoicePromptDTO> prompts;
}
