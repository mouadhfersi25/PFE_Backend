package com.britechnology.edugame.dto.game;

import com.britechnology.edugame.entity.EtatJeu;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ChangeGameStatusRequest {
    @NotNull
    private EtatJeu etat;
    private String motifRefus;
}
