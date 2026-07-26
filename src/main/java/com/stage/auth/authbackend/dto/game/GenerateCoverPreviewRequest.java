package com.stage.auth.authbackend.dto.game;

import com.stage.auth.authbackend.entity.TypeJeu;
import lombok.Data;

@Data
public class GenerateCoverPreviewRequest {
    private String titre;
    private String description;
    private TypeJeu typeJeu;
    private Integer ageMin;
    private Integer ageMax;
    private Integer difficulte;
}

