package com.britechnology.edugame.dto.voice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateVoicePromptRequest {
    private String texteReference;
    private Integer ordre;
    private String sousType;
    private String tolerance;
    private String indice;
    private Integer dureeMaxSecondes;
}
