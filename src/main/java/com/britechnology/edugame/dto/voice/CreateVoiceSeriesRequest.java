package com.britechnology.edugame.dto.voice;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVoiceSeriesRequest {
    @NotBlank
    private String titre;
    private String description;
    private String langue;
    private Integer difficulte;
}
