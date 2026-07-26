package com.britechnology.edugame.dto.educator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateLogicPuzzleRequest {

    @NotNull
    private Long jeuId;

    @NotBlank
    private String enonce;

    private String sousType;
    private String donnees;

    @NotBlank
    private String bonneReponse;

    private String indice;
    private Integer difficulte;
}
