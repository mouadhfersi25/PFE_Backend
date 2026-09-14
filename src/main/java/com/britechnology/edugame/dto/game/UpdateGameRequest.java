package com.britechnology.edugame.dto.game;

import com.britechnology.edugame.entity.Difficulte;
import com.britechnology.edugame.entity.ModeJeu;
import com.britechnology.edugame.entity.QuizVariant;
import com.britechnology.edugame.entity.TypeJeu;
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

    private Difficulte difficulte;

    @Min(0)
    private Integer ageMin;

    @Min(0)
    private Integer ageMax;

    private TypeJeu typeJeu;
    private ModeJeu modeJeu;

    @Min(1)
    @Max(999)
    private Integer dureeMinutes;

    private String coverImageUrl;

    private Boolean actif;

    private QuizVariant quizVariant;
}
