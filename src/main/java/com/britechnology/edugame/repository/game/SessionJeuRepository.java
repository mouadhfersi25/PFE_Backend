package com.britechnology.edugame.repository.game;

import com.britechnology.edugame.dto.admin.AdminGamePerformanceDTO;
import com.britechnology.edugame.dto.admin.AdminScoringDistributionDTO;
import com.britechnology.edugame.entity.EtatSession;
import com.britechnology.edugame.entity.SessionJeu;
import com.britechnology.edugame.entity.TypeJeu;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SessionJeuRepository extends JpaRepository<SessionJeu, Long> {
    long countByEtatSession(EtatSession etatSession);
    long countByUtilisateurIdAndJeuIdAndDateDebutAfter(Long userId, Long gameId, LocalDateTime dateDebutAfter);
    long countByUtilisateurIdAndEtatSession(Long userId, EtatSession etatSession);
    long countByUtilisateurIdAndEtatSessionAndJeuTypeJeu(Long userId, EtatSession etatSession, TypeJeu typeJeu);
    List<SessionJeu> findTop12ByUtilisateurIdOrderByDateDebutDesc(Long userId);
    List<SessionJeu> findTop120ByUtilisateurIdOrderByDateDebutDesc(Long userId);
    boolean existsByUtilisateurIdAndJeuIdAndRoomCodeAndModeJeuLance(
            Long utilisateurId,
            Long jeuId,
            String roomCode,
            String modeJeuLance
    );
    List<SessionJeu> findByJeuIdAndRoomCodeAndModeJeuLanceAndEtatSessionOrderByDateFinAsc(
            Long jeuId,
            String roomCode,
            String modeJeuLance,
            EtatSession etatSession
    );

    /** Sérialise les soumissions d'une même room jusqu'au commit PostgreSQL. */
    @Query(value = "select pg_advisory_xact_lock(:lockKey)", nativeQuery = true)
    void lockOnlineRoom(@Param("lockKey") long lockKey);

    @Query("""
            select coalesce(avg(s.accuracyPercent), 0)
            from SessionJeu s
            where s.utilisateur.id = :userId
              and s.etatSession = com.britechnology.edugame.entity.EtatSession.TERMINE
              and s.jeu.typeJeu = :typeJeu
              and s.accuracyPercent is not null
              and s.accuracyPercent >= 0
            """)
    Double averageAccuracyByUserAndType(@Param("userId") Long userId, @Param("typeJeu") TypeJeu typeJeu);

    @Query("""
            select coalesce(sum(s.durationSeconds), 0)
            from SessionJeu s
            where s.utilisateur.id = :userId
              and s.etatSession = com.britechnology.edugame.entity.EtatSession.TERMINE
              and s.dateDebut >= :fromDate
            """)
    Integer sumDurationSecondsSince(@Param("userId") Long userId, @Param("fromDate") LocalDateTime fromDate);

    @Query("""
            select case when count(s) > 0 then true else false end
            from SessionJeu s
            where s.utilisateur.id = :userId
              and s.etatSession = com.britechnology.edugame.entity.EtatSession.TERMINE
              and (
                    coalesce(s.scoreFinal, s.scoreGlobal, 0) > 0
                    or coalesce(s.accuracyPercent, 0) >= 60
              )
            """)
    boolean existsAnyWinByUser(@Param("userId") Long userId);

    @Query("""
            select case when count(s) > 0 then true else false end
            from SessionJeu s
            where s.utilisateur.id = :userId
              and s.etatSession = com.britechnology.edugame.entity.EtatSession.TERMINE
              and s.jeu.typeJeu = com.britechnology.edugame.entity.TypeJeu.QUIZ
              and (
                    coalesce(s.scoreFinal, s.scoreGlobal, 0) > 0
                    or coalesce(s.accuracyPercent, 0) >= 60
              )
            """)
    boolean existsQuizWinByUser(@Param("userId") Long userId);

    @Query("""
            select case when count(s) > 0 then true else false end
            from SessionJeu s
            where s.utilisateur.id = :userId
              and s.etatSession = com.britechnology.edugame.entity.EtatSession.TERMINE
              and coalesce(s.accuracyPercent, 0) >= 100
            """)
    boolean existsPerfectGameByUser(@Param("userId") Long userId);

    @Query("""
            select new com.britechnology.edugame.dto.admin.AdminScoringDistributionDTO(
                j.id,
                j.titre,
                j.typeJeu,
                count(s),
                coalesce(avg(s.scoreGlobal), 0),
                coalesce(min(s.scoreGlobal), 0),
                coalesce(max(s.scoreGlobal), 0),
                coalesce(avg(s.xpGained), 0),
                coalesce(sum(s.xpGained), 0),
                coalesce(avg(coalesce(s.scoreFinal, s.scoreGlobal) - coalesce(s.scoreBase, s.scoreGlobal)), 0),
                coalesce(sum(case when s.anomalyNotes is not null and length(trim(s.anomalyNotes)) > 0 then 1 else 0 end), 0)
            )
            from SessionJeu s
            join s.jeu j
            where s.etatSession = com.britechnology.edugame.entity.EtatSession.TERMINE
            group by j.id, j.titre, j.typeJeu
            order by count(s) desc
            """)
    List<AdminScoringDistributionDTO> fetchScoringDistributionByGame();

    @Query(value = """
            select cast(coalesce(s.date_debut, s.date_fin) as date) as d, count(s.id)
            from sessions_jeu s
            where coalesce(s.date_debut, s.date_fin) >= :fromInclusive
              and coalesce(s.date_debut, s.date_fin) < :toExclusive
            group by cast(coalesce(s.date_debut, s.date_fin) as date)
            order by d
            """, nativeQuery = true)
    List<Object[]> countSessionsByStartDate(
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive);

    @Query("""
            select s from SessionJeu s
            join fetch s.utilisateur
            join fetch s.jeu
            order by coalesce(s.dateDebut, s.dateFin) desc
            """)
    List<SessionJeu> findRecentWithDetails(Pageable pageable);

    /**
     * Toutes les sessions par jeu (tous états) pour volume ; moyennes utiles sur données présentes.
     */
    @Query("""
            select new com.britechnology.edugame.dto.admin.AdminGamePerformanceDTO(
                j.titre,
                count(s),
                coalesce(avg(s.scoreGlobal), 0),
                coalesce(avg(coalesce(s.accuracyPercent, 0)), 0)
            )
            from SessionJeu s
            join s.jeu j
            group by j.id, j.titre
            order by count(s) desc
            """)
    List<AdminGamePerformanceDTO> fetchGamePerformanceStats();

    @Query(value = """
            select sub.age_bucket,
                   coalesce(avg(sub.score_global), 0),
                   count(distinct sub.uid)
            from (
                select
                    case
                        when extract(year from age(current_date, u.date_de_naissance)) < 8 then '0-7'
                        when extract(year from age(current_date, u.date_de_naissance)) < 13 then '8-12'
                        when extract(year from age(current_date, u.date_de_naissance)) < 18 then '13-17'
                        else '18+'
                    end as age_bucket,
                    case when s.etat_session = 'TERMINE' then s.score_global else null end as score_global,
                    u.id as uid
                from sessions_jeu s
                join users u on u.id = s.id_utilisateur
                where u.date_de_naissance is not null
            ) sub
            group by sub.age_bucket
            """, nativeQuery = true)
    List<Object[]> aggregatePerformanceByAgeBucket();

    @Query("select count(s) from SessionJeu s")
    long countAllSessions();

    @Query("select count(distinct s.utilisateur.id) from SessionJeu s where coalesce(s.dateDebut, s.dateFin) >= :since")
    long countDistinctPlayersSince(@Param("since") LocalDateTime since);

    @Query(value = """
            select coalesce(avg(user_total), 0)
            from (
                select sum(coalesce(s.duration_seconds, 0)) as user_total
                from sessions_jeu s
                where s.etat_session = 'TERMINE'
                group by s.id_utilisateur
            ) t
            where user_total > 0
            """, nativeQuery = true)
    Double avgTotalPlaytimeSecondsPerActiveUser();

    @Query("""
            select coalesce(avg(
                case
                    when s.accuracyPercent is not null and s.accuracyPercent >= 0
                        then cast(s.accuracyPercent as double)
                    when s.totalQuestions is not null and s.totalQuestions > 0
                        and s.correctAnswers is not null
                        then (100.0 * s.correctAnswers / s.totalQuestions)
                    else null
                end
            ), 0)
            from SessionJeu s
            where s.etatSession = com.britechnology.edugame.entity.EtatSession.TERMINE
            """)
    Double averageSuccessRatePercent();

    @Query("""
            select coalesce(avg(
                case
                    when s.accuracyPercent is not null and s.accuracyPercent >= 0
                        then cast(s.accuracyPercent as double)
                    when s.totalQuestions is not null and s.totalQuestions > 0
                        and s.correctAnswers is not null
                        then (100.0 * s.correctAnswers / s.totalQuestions)
                    else null
                end
            ), 0)
            from SessionJeu s
            where s.etatSession = com.britechnology.edugame.entity.EtatSession.TERMINE
              and coalesce(s.dateFin, s.dateDebut) >= :fromInclusive
              and coalesce(s.dateFin, s.dateDebut) < :toExclusive
            """)
    Double averageSuccessRatePercentBetween(
            @Param("fromInclusive") LocalDateTime fromInclusive,
            @Param("toExclusive") LocalDateTime toExclusive);

    @Query("""
            select coalesce(sum(s.totalQuestions), 0)
            from SessionJeu s
            where s.etatSession = com.britechnology.edugame.entity.EtatSession.TERMINE
              and s.totalQuestions is not null
              and s.totalQuestions > 0
            """)
    long sumTotalQuizAnswers();

    @Query("""
            select j.typeJeu,
                   count(s),
                   coalesce(avg(
                       case
                           when s.accuracyPercent is not null and s.accuracyPercent >= 0
                               then cast(s.accuracyPercent as double)
                           when s.totalQuestions is not null and s.totalQuestions > 0
                               and s.correctAnswers is not null
                               then (100.0 * s.correctAnswers / s.totalQuestions)
                           else null
                       end
                   ), 0)
            from SessionJeu s
            join s.jeu j
            where s.etatSession = com.britechnology.edugame.entity.EtatSession.TERMINE
            group by j.typeJeu
            order by count(s) desc
            """)
    List<Object[]> aggregateSessionStatsByGameType();
}

