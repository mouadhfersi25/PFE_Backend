package com.britechnology.edugame.service.educator;

import com.britechnology.edugame.dto.educator.EducatorGameTypeStatsDTO;
import com.britechnology.edugame.dto.educator.EducatorLearningStatsDTO;
import com.britechnology.edugame.entity.TypeJeu;
import com.britechnology.edugame.repository.game.SessionJeuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class EducatorLearningStatisticsService {

    private static final Map<TypeJeu, String> TYPE_LABELS = Map.of(
            TypeJeu.QUIZ, "Quiz",
            TypeJeu.MEMOIRE, "Mémoire",
            TypeJeu.LOGIQUE, "Logique",
            TypeJeu.REFLEXE, "Réflexe"
    );

    private static final Map<TypeJeu, String> TYPE_COLORS = Map.of(
            TypeJeu.QUIZ, "#10b981",
            TypeJeu.MEMOIRE, "#a855f7",
            TypeJeu.LOGIQUE, "#3b82f6",
            TypeJeu.REFLEXE, "#f59e0b"
    );

    private final SessionJeuRepository sessionJeuRepository;

    @Transactional(readOnly = true)
    public EducatorLearningStatsDTO getLearningStats() {
        Double avgRate = sessionJeuRepository.averageSuccessRatePercent();
        long totalAnswers = sessionJeuRepository.sumTotalQuizAnswers();

        YearMonth current = YearMonth.now();
        YearMonth previous = current.minusMonths(1);
        Double thisMonth = sessionJeuRepository.averageSuccessRatePercentBetween(
                current.atDay(1).atStartOfDay(),
                current.plusMonths(1).atDay(1).atStartOfDay());
        Double lastMonth = sessionJeuRepository.averageSuccessRatePercentBetween(
                previous.atDay(1).atStartOfDay(),
                current.atDay(1).atStartOfDay());

        double improvement = computeImprovement(thisMonth, lastMonth);

        return EducatorLearningStatsDTO.builder()
                .avgSuccessRate(roundPercent(avgRate))
                .totalAnswers(totalAnswers)
                .improvementPercent(Math.round(improvement * 10.0) / 10.0)
                .sessionsByGameType(buildSessionsByGameType())
                .build();
    }

    private List<EducatorGameTypeStatsDTO> buildSessionsByGameType() {
        Map<TypeJeu, long[]> aggregated = new EnumMap<>(TypeJeu.class);
        for (Object[] row : sessionJeuRepository.aggregateSessionStatsByGameType()) {
            if (row[0] == null) {
                continue;
            }
            TypeJeu type = row[0] instanceof TypeJeu t ? t : TypeJeu.valueOf(String.valueOf(row[0]));
            long sessions = row[1] instanceof Number n ? n.longValue() : 0L;
            double avg = row[2] instanceof Number n ? n.doubleValue() : 0;
            aggregated.put(type, new long[]{sessions, Math.round(avg)});
        }

        List<EducatorGameTypeStatsDTO> result = new ArrayList<>();
        for (TypeJeu type : TypeJeu.values()) {
            long[] stats = aggregated.getOrDefault(type, new long[]{0, 0});
            result.add(EducatorGameTypeStatsDTO.builder()
                    .gameType(type.name())
                    .label(TYPE_LABELS.getOrDefault(type, type.name()))
                    .sessions(stats[0])
                    .avgSuccessRate((int) Math.min(100, stats[1]))
                    .color(TYPE_COLORS.getOrDefault(type, "#94a3b8"))
                    .build());
        }
        result.sort((a, b) -> Long.compare(b.getSessions(), a.getSessions()));
        return result;
    }

    static int roundPercent(Double value) {
        if (value == null || value.isNaN() || value < 0) {
            return 0;
        }
        return (int) Math.round(Math.min(100.0, value));
    }

    static double computeImprovement(Double thisMonth, Double lastMonth) {
        double current = thisMonth == null ? 0 : thisMonth;
        double previous = lastMonth == null ? 0 : lastMonth;
        if (previous <= 0) {
            return current > 0 ? current : 0;
        }
        return current - previous;
    }
}
