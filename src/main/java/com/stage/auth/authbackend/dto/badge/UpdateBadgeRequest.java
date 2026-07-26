package com.stage.auth.authbackend.dto.badge;

import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * Body de la requête PUT /api/admin/badges/{id} (mise à jour d'un badge).
 * Tous les champs optionnels ; seuls ceux envoyés sont mis à jour.
 */
@Data
public class UpdateBadgeRequest {

    @Size(max = 150)
    private String nom;

    @Size(max = 255)
    private String description;

    @Size(max = 50)
    private String typeCondition;

    private Integer scoreCondition;

    @Size(max = 255)
    private String icone;
}
