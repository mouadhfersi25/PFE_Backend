package com.stage.auth.authbackend.dto.game;

import com.stage.auth.authbackend.entity.ModeJeu;
import com.stage.auth.authbackend.entity.TypeJeu;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Body de la requête PUT /api/admin/games/{id} (mise à jour d'un jeu).
 * Tous les champs sont optionnels ; seuls ceux envoyés sont mis à jour.
 */
@Data
public class UpdateGameRequest {

    @Size(max = 200)
    private String titre;

    @Size(max = 5000)
    private String description;

    @Min(0)
    @Max(10)
    private Integer difficulte;

    @Min(0)
    private Integer ageMin;

    @Min(0)
    private Integer ageMax;

    private TypeJeu typeJeu;
    private ModeJeu modeJeu;

    @Min(1)
    @Max(999)
    private Integer dureeMinutes;

    @Size(max = 20)
    private String icone;

    private String coverImageUrl;

    private Boolean actif;
}
