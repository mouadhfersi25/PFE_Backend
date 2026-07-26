package com.stage.auth.authbackend.dto.educator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body PUT /api/educator/questions/:id. Frontend: UpdateQuizQuestionRequest (contenu?, bonneReponse?, options?, explication?, difficulte?). Tous optionnels.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateQuizQuestionRequest {

    private String contenu;
    private String bonneReponse;
    private java.util.List<String> options;
    private String sousType;
    private String mediaUrl;
    private String promptAudioUrl;
    private String explication;
    private Integer difficulte;
}

