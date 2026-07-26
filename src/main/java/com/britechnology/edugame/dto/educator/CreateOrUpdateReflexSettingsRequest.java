package com.britechnology.edugame.dto.educator;

import com.britechnology.edugame.entity.ReflexModel;
import com.britechnology.edugame.entity.ReflexStimulusType;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrUpdateReflexSettingsRequest {

    @NotNull
    private Long jeuId;

    @NotNull
    private Integer nombreRounds;

    private Integer tempsReactionMaxMs;
    private ReflexStimulusType typeStimuli;
    private ReflexModel modeleReflexe;
    private Integer noGoRatio;
    private Integer choiceTargetCount;
    private Integer difficulte;
}
