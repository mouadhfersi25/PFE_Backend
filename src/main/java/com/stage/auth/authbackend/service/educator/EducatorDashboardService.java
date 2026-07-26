package com.stage.auth.authbackend.service.educator;

import com.stage.auth.authbackend.dto.educator.EducatorDashboardStatsDTO;
import com.stage.auth.authbackend.repository.game.JeuRepository;
import com.stage.auth.authbackend.repository.game.QuestionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * Statistiques du dashboard éducateur. Chaque champ correspond à la BDD :
 * - totalQuestionsCreated : table {@code questions} (COUNT)
 * - assignedGames         : table {@code jeux} (COUNT)
 * - difficultyDistribution: colonne {@code questions.difficulte} (1=Easy, 2=Medium, 3=Hard)
 * - avgSuccessRate        : à terme table {@code statistiques_performance.taux_reussite} (moyenne)
 * - studentActivity       : à terme table {@code sessions_jeu} (COUNT ou utilisateurs distincts)
 */
@Service
@RequiredArgsConstructor
public class EducatorDashboardService {

    private final QuestionRepository questionRepository;
    private final JeuRepository jeuRepository;

    public EducatorDashboardStatsDTO getStats() {
        // BDD: table questions
        long totalQuestions = questionRepository.count();
        // BDD: table jeux
        long assignedGames = jeuRepository.count();

        // BDD: questions.difficulte (1=Easy, 2=Medium, 3=Hard)
        long easy = questionRepository.countByDifficulte(1);
        long medium = questionRepository.countByDifficulte(2);
        long hard = questionRepository.countByDifficulte(3);

        List<EducatorDashboardStatsDTO.DifficultyCountDTO> distribution = new ArrayList<>();
        distribution.add(new EducatorDashboardStatsDTO.DifficultyCountDTO("Easy", easy, "#10b981"));
        distribution.add(new EducatorDashboardStatsDTO.DifficultyCountDTO("Medium", medium, "#f59e0b"));
        distribution.add(new EducatorDashboardStatsDTO.DifficultyCountDTO("Hard", hard, "#ef4444"));

        return EducatorDashboardStatsDTO.builder()
                .totalQuestionsCreated(totalQuestions)
                .assignedGames(assignedGames)
                .avgSuccessRate(0)
                .studentActivity(0)
                .difficultyDistribution(distribution)
                .build();
    }
}
