package com.stage.auth.authbackend.dto.educator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body POST /api/educator/memory-cards. Frontend: CreateMemoryCardRequest (jeuId, symbole, pairKey?, categorie?).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateMemoryCardRequest {

    @NotNull
    private Long jeuId;

    @NotBlank
    private String symbole;

    private String cardType;
    private String cardValue;
    private String sousType;

    private String pairKey;
    private String categorie;
}
