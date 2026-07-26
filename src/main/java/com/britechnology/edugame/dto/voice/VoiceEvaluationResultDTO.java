package com.britechnology.edugame.dto.voice;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceEvaluationResultDTO {
    private Long attemptId;
    private Long sessionOralId;
    private Long promptId;
    private String transcription;
    private Integer scoreContenu;
    private Boolean reussite;
    private Integer dureeSecondes;
    private List<String> expectedWords;
    private List<String> spokenWords;
    private List<String> correctWords;
    private List<String> missedWords;
    private List<String> extraWords;
}
