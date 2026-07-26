package com.britechnology.edugame.dto.educator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO renvoyé par GET /api/educator/questions?gameId= et GET/POST/PUT /api/educator/questions/:id.
 * Frontend: QuizQuestionDTO (id, jeuId, jeuTitre, contenu, bonneReponse, options[], explication, difficulte).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QuizQuestionDTO {

    private Long id;
    private Long jeuId;
    private String jeuTitre;

    private String contenu;
    private String bonneReponse;
    /** Liste des options de réponse (QCM). */
    private java.util.List<String> options;
    private String sousType;
    private String mediaUrl;
    private String promptAudioUrl;
    private String explication;
    private Integer difficulte;
}

