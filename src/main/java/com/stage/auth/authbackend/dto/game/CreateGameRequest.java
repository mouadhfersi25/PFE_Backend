package com.stage.auth.authbackend.dto.game;

import com.stage.auth.authbackend.entity.ModeJeu;
import com.stage.auth.authbackend.entity.TypeJeu;
import jakarta.validation.constraints.*;
import lombok.Data;

/**
 * Body de la requête POST /api/admin/games (création d'un jeu).
 */
@Data
public class CreateGameRequest {

    @NotBlank(message = "Le titre est obligatoire")
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

    @NotNull(message = "Le type de jeu est obligatoire")
    private TypeJeu typeJeu;

    @NotNull(message = "Le mode de jeu est obligatoire")
    private ModeJeu modeJeu;

    /** Durée estimée en minutes (optionnel). */
    @Min(1)
    @Max(999)
    private Integer dureeMinutes;

    @Size(max = 20)
    private String icone;

    private String coverImageUrl;

    /** Par défaut true si non fourni. */
    private Boolean actif = true;
}
