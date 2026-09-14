package com.britechnology.edugame.dto.game;

import com.britechnology.edugame.entity.Difficulte;
import com.britechnology.edugame.entity.ModeJeu;
import com.britechnology.edugame.entity.QuizVariant;
import com.britechnology.edugame.entity.TypeJeu;
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

    private Difficulte difficulte;

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

    private String coverImageUrl;

    /** Par défaut true si non fourni. */
    private Boolean actif = true;

    /** Variante pédagogique quiz (ignorée si typeJeu != QUIZ). */
    private QuizVariant quizVariant;
}
