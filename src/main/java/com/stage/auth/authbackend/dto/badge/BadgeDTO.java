package com.stage.auth.authbackend.dto.badge;

import lombok.*;

/**
 * DTO pour l'API Manage Badges.
 * Aligné sur l'entité Badge (table badges).
 * Réponse : GET /api/admin/badges, GET /api/admin/badges/{id}.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class BadgeDTO {

    private Long id;

    private String nom;
    private String description;

    /** Type de condition (SCORE_MIN, FIRST_WIN, GAMES_PLAYED, etc.). */
    private String typeCondition;

    /** Valeur (ex. score min, nombre de parties) selon le type. */
    private Integer scoreCondition;

    /** Nom ou URL de l'icône (optionnel). */
    private String icone;
}
