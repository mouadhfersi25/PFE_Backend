package com.britechnology.edugame.dto.game;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

/** Body de PATCH /api/admin/games/{id}/reactivation/reject. */
@Getter
@Setter
public class RejectReactivationRequest {
    @NotBlank(message = "Le motif de refus est obligatoire")
    private String motif;
}
