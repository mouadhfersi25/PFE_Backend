package com.britechnology.edugame.dto.game;

import com.britechnology.edugame.entity.TypeJeu;
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

