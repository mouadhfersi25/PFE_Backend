package com.stage.auth.authbackend.repository.game;

import com.stage.auth.authbackend.dto.admin.AdminScoringDistributionDTO;
import com.stage.auth.authbackend.dto.player.TeamLeaderboardEntryDTO;
import com.stage.auth.authbackend.entity.EtatSession;
import com.stage.auth.authbackend.entity.SessionJeu;
import com.stage.auth.authbackend.entity.TypeJeu;
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

    @Query("""
            select coalesce(sum(s.durationSeconds), 0)
            from SessionJeu s
            where s.utilisateur.id = :userId
              and s.etatSession = com.stage.auth.authbackend.entity.EtatSession.TERMINE
              and s.dateDebut >= :fromDate
            """)
    Integer sumDurationSecondsSince(@Param("userId") Long userId, @Param("fromDate") LocalDateTime fromDate);

    @Query("""
            select case when count(s) > 0 then true else false end
            from SessionJeu s
            where s.utilisateur.id = :userId
              and s.etatSession = com.stage.auth.authbackend.entity.EtatSession.TERMINE
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
              and s.etatSession = com.stage.auth.authbackend.entity.EtatSession.TERMINE
              and s.jeu.typeJeu = com.stage.auth.authbackend.entity.TypeJeu.QUIZ
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
              and s.etatSession = com.stage.auth.authbackend.entity.EtatSession.TERMINE
              and coalesce(s.accuracyPercent, 0) >= 100
            """)
    boolean existsPerfectGameByUser(@Param("userId") Long userId);

    @Query("""
            select new com.stage.auth.authbackend.dto.admin.AdminScoringDistributionDTO(
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
            where s.etatSession = com.stage.auth.authbackend.entity.EtatSession.TERMINE
            group by j.id, j.titre, j.typeJeu
            order by count(s) desc
            """)
    List<AdminScoringDistributionDTO> fetchScoringDistributionByGame();

    @Query("""
            select new com.stage.auth.authbackend.dto.player.TeamLeaderboardEntryDTO(
                case
                    when s.teamName is null or length(trim(s.teamName)) = 0
                        then concat('Equipe ', upper(coalesce(s.roomCode, 'XXXXXX')))
                    else s.teamName
                end,
                upper(coalesce(s.roomCode, '')),
                count(s),
                count(distinct s.utilisateur.id),
                coalesce(avg(coalesce(s.scoreFinal, s.scoreGlobal)), 0),
                coalesce(sum(coalesce(s.scoreFinal, s.scoreGlobal)), 0)
            )
            from SessionJeu s
            where s.etatSession = com.stage.auth.authbackend.entity.EtatSession.TERMINE
              and upper(coalesce(s.modeJeuLance, '')) = 'COLLECTIF'
              and s.roomCode is not null
            group by s.teamName, s.roomCode
            order by coalesce(sum(coalesce(s.scoreFinal, s.scoreGlobal)), 0) desc,
                     count(distinct s.utilisateur.id) desc,
                     upper(coalesce(s.roomCode, '')) asc
            """)
    List<TeamLeaderboardEntryDTO> fetchTeamLeaderboard();
}

