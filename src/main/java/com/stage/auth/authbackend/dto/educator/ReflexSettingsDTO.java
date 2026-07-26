package com.stage.auth.authbackend.dto.educator;

import com.stage.auth.authbackend.entity.ReflexModel;
import com.stage.auth.authbackend.entity.ReflexStimulusType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReflexSettingsDTO {

    private Long id;
    private Long jeuId;
    private String jeuTitre;

    private Integer nombreRounds;
    private Integer tempsReactionMaxMs;
    private ReflexStimulusType typeStimuli;
    private ReflexModel modeleReflexe;
    private Integer noGoRatio;
    private Integer choiceTargetCount;
    private Integer difficulte;
}
