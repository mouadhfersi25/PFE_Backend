package com.stage.auth.authbackend.dto.educator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateLogicPuzzleRequest {

    private String enonce;
    private String sousType;
    private String donnees;
    private String bonneReponse;
    private String indice;
    private Integer difficulte;
}
