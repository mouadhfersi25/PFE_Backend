package com.britechnology.edugame.dto.educator;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body POST /api/educator/questions. Frontend: CreateQuizQuestionRequest (jeuId, contenu, bonneReponse, options?, explication?, difficulte?).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateQuizQuestionRequest {

    @NotNull
    private Long jeuId;

    @NotBlank
    private String contenu;

    @NotBlank
    private String bonneReponse;

    /** Liste des options de réponse (QCM). Si présent, bonneReponse doit correspondre à l'une des options. */
    private java.util.List<String> options;

    private String sousType;
    private String mediaUrl;
    private String promptAudioUrl;

    private String explication;

    private Integer difficulte;
}

