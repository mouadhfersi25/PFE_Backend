package com.stage.auth.authbackend.dto.game;

import com.stage.auth.authbackend.entity.EtatJeu;
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
