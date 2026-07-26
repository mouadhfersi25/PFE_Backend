package com.stage.auth.authbackend.dto.badge;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body de la requête POST /api/admin/badges (création d'un badge).
 * Champs alignés sur l'entité Badge.
 */
@Data
public class CreateBadgeRequest {

    @NotBlank(message = "Le nom du badge est obligatoire")
    @Size(max = 150)
    private String nom;

    @Size(max = 255)
    private String description;

    /** Type de condition (SCORE_MIN, FIRST_WIN, GAMES_PLAYED, STREAK_DAYS, QUIZ_WIN, PERFECT_GAME). Obligatoire. */
    @NotBlank(message = "Le type de condition est obligatoire")
    @Size(max = 50)
    private String typeCondition;

    /** Valeur (ex. score min, nombre) selon le type ; optionnel pour FIRST_WIN, QUIZ_WIN, PERFECT_GAME. */
    private Integer scoreCondition;

    @Size(max = 255)
    private String icone;
}
