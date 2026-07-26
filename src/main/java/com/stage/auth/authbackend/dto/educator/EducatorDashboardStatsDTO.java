package com.stage.auth.authbackend.dto.educator;

import lombok.*;

import java.util.List;

/**
 * Stats du dashboard éducateur. Champs alignés BDD :
 * totalQuestionsCreated -> questions (count),
 * assignedGames -> jeux (count),
 * difficultyDistribution -> questions.difficulte (1=Easy, 2=Medium, 3=Hard),
 * avgSuccessRate -> statistiques_performance.taux_reussite (moyenne, optionnel),
 * studentActivity -> sessions_jeu (count, optionnel).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EducatorDashboardStatsDTO {

    /** Nombre de questions en base (table questions). */
    private long totalQuestionsCreated;
    /** Nombre de jeux en base (table jeux). */
    private long assignedGames;
    /** Taux de réussite moyen (table statistiques_performance.taux_reussite). */
    private int avgSuccessRate;
    /** Activité élèves (table sessions_jeu). */
    private long studentActivity;
    /** Répartition par difficulté (colonne questions.difficulte : 1=Easy, 2=Medium, 3=Hard). */
    private List<DifficultyCountDTO> difficultyDistribution;

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class DifficultyCountDTO {
        private String name;  // "Easy", "Medium", "Hard"
        private long value;   // count pour questions.difficulte = 1, 2 ou 3
        private String color;
    }
}
