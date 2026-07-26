package com.britechnology.edugame.dto.voice;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateVoicePromptRequest {
    @NotNull
    private Long seriesId;

    @NotBlank
    private String texteReference;

    private Integer ordre;
    private String sousType;
    private String tolerance;
    private String indice;
    private Integer dureeMaxSecondes;
}
