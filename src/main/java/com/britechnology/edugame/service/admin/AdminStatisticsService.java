package com.britechnology.edugame.service.admin;

import com.britechnology.edugame.dto.admin.*;
import com.britechnology.edugame.entity.EtatSession;
import com.britechnology.edugame.entity.SessionJeu;
import com.britechnology.edugame.entity.User;
import com.britechnology.edugame.repository.game.SessionJeuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.sql.Date;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminStatisticsService {

    private static final List<String> AGE_BUCKET_ORDER = List.of("0-7", "8-12", "13-17", "18+");

    /** Fenêtre glissante pour l’histogramme admin (évite graphes vides si peu d’activité récente). */
    private static final int ROLLING_SESSION_DAY_WINDOW = 30;

    private final SessionJeuRepository sessionJeuRepository;

    public List<AdminDaySessionCountDTO> getSessionsLast7Days() {
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(ROLLING_SESSION_DAY_WINDOW - 1);
        LocalDateTime fromInclusive = start.atStartOfDay();
        LocalDateTime toExclusive = end.plusDays(1).atStartOfDay();
        List<Object[]> rows = sessionJeuRepository.countSessionsByStartDate(fromInclusive, toExclusive);
        Map<LocalDate, Long> byDay = new LinkedHashMap<>();
        for (Object[] row : rows) {
            LocalDate d;
            if (row[0] instanceof Date sqlDate) {
                d = sqlDate.toLocalDate();
            } else if (row[0] instanceof LocalDate ld) {
                d = ld;
            } else {
                continue;
            }
            long n = row[1] instanceof Number num ? num.longValue() : 0L;
            byDay.put(d, n);
        }
        DateTimeFormatter shortDay = DateTimeFormatter.ofPattern("EEE", Locale.ENGLISH);
        List<AdminDaySessionCountDTO> out = new ArrayList<>();
        for (LocalDate d = start; !d.isAfter(end); d = d.plusDays(1)) {
            out.add(AdminDaySessionCountDTO.builder()
                    .day(d.format(shortDay))
                    .date(d.toString())
                    .sessions(byDay.getOrDefault(d, 0L))
                    .build());
        }
        return out;
    }

    public List<AdminRecentActivityDTO> getRecentActivity(int limit) {
        int safe = Math.max(1, Math.min(limit, 100));
        var pageable = PageRequest.of(0, safe, Sort.by(Sort.Direction.DESC, "dateDebut"));
        List<SessionJeu> sessions = sessionJeuRepository.findRecentWithDetails(pageable);
        DateTimeFormatter iso = DateTimeFormatter.ISO_LOCAL_DATE_TIME;
        return sessions.stream()
                .map(s -> AdminRecentActivityDTO.builder()
                        .id(String.valueOf(s.getId()))
                        .player(formatPlayer(s.getUtilisateur()))
                        .action(actionLabel(s.getEtatSession()))
                        .game(s.getJeu() != null ? s.getJeu().getTitre() : "—")
                        .occurredAt(formatSessionInstant(s, iso))
                        .build())
                .collect(Collectors.toList());
    }

    public AdminStatisticsOverviewDTO getOverview() {
        List<AdminGamePerformanceDTO> gamePerformance = sessionJeuRepository.fetchGamePerformanceStats();
        Map<String, Object[]> ageRaw = new LinkedHashMap<>();
        for (Object[] row : sessionJeuRepository.aggregatePerformanceByAgeBucket()) {
            if (row[0] == null) {
                continue;
            }
            ageRaw.put(String.valueOf(row[0]).trim(), row);
        }
        List<AdminAgeGroupStatDTO> ageGroups = new ArrayList<>();
        for (String key : AGE_BUCKET_ORDER) {
            Object[] r = ageRaw.get(key);
            if (r != null) {
                ageGroups.add(AdminAgeGroupStatDTO.builder()
                        .age(key)
                        .avgScore(r[1] instanceof Number n ? n.doubleValue() : 0)
                        .players(r[2] instanceof Number n ? n.longValue() : 0L)
                        .build());
            } else {
                ageGroups.add(AdminAgeGroupStatDTO.builder()
                        .age(key)
                        .avgScore(0)
                        .players(0)
                        .build());
            }
        }

        long totalSessions = sessionJeuRepository.countAllSessions();
        long termine = sessionJeuRepository.countByEtatSession(EtatSession.TERMINE);
        double completionPct = totalSessions == 0 ? 0 : (100.0 * termine / totalSessions);

        LocalDateTime since30 = LocalDateTime.now().minusDays(30);
        long activePlayers = sessionJeuRepository.countDistinctPlayersSince(since30);

        Double avgSecs = sessionJeuRepository.avgTotalPlaytimeSecondsPerActiveUser();
        double minutes = (avgSecs == null || avgSecs <= 0) ? 0 : (avgSecs / 60.0);

        AdminGlobalMetricsDTO metrics = AdminGlobalMetricsDTO.builder()
                .overallCompletionRatePercent(Math.round(completionPct * 10.0) / 10.0)
                .activePlayers(activePlayers)
                .avgPlaytimeMinutesPerUser(Math.round(minutes * 10.0) / 10.0)
                .build();

        return AdminStatisticsOverviewDTO.builder()
                .gamePerformance(gamePerformance)
                .ageGroups(ageGroups)
                .metrics(metrics)
                .build();
    }

    private static String formatSessionInstant(SessionJeu s, DateTimeFormatter iso) {
        var t = s.getDateDebut() != null ? s.getDateDebut() : s.getDateFin();
        return t != null ? t.format(iso) : "";
    }

    private static String formatPlayer(User u) {
        if (u == null) {
            return "—";
        }
        String p = u.getPrenom() != null ? u.getPrenom().trim() : "";
        String n = u.getNom() != null ? u.getNom().trim() : "";
        String full = (p + " " + n).trim();
        if (!full.isEmpty()) {
            return full;
        }
        return u.getEmail() != null ? u.getEmail() : "—";
    }

    private static String actionLabel(EtatSession e) {
        if (e == null) {
            return "—";
        }
        return switch (e) {
            case TERMINE -> "Terminé";
            case EN_COURS -> "En cours";
            case ABANDONNE -> "Abandonné";
        };
    }
}
