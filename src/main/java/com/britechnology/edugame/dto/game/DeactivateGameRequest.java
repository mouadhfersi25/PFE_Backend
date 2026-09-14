package com.britechnology.edugame.dto.game;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Body de PATCH /api/admin/games/{id}/deactivate. */
@Getter
@Setter
public class DeactivateGameRequest {
    @NotBlank(message = "Les détails de désactivation sont obligatoires")
    private String motif;
}
