package com.britechnology.edugame.dto.educator;

import lombok.*;

import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EducatorLearningStatsDTO {
    /** Taux de réussite moyen (%) sur les sessions terminées. */
    private int avgSuccessRate;
    /** Nombre total de réponses enregistrées (somme des questions par session quiz). */
    private long totalAnswers;
    /** Évolution du taux de réussite vs le mois précédent (points de pourcentage). */
    private double improvementPercent;
    /** Sessions terminées et taux de réussite moyen par type de jeu. */
    private List<EducatorGameTypeStatsDTO> sessionsByGameType;
}
