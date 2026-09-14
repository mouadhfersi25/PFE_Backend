package com.britechnology.edugame.repository.voice;

import com.britechnology.edugame.entity.SessionOral;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface SessionOralRepository extends JpaRepository<SessionOral, Long> {
    List<SessionOral> findTop50ByUtilisateurIdOrderByDateDebutDesc(Long utilisateurId);

    long countByUtilisateurIdAndSeriesIdAndDateDebutAfter(Long utilisateurId, Long seriesId, LocalDateTime after);

    void deleteBySeriesId(Long seriesId);

    /** Score gagné par le joueur via l'atelier oral depuis une date donnée (ex: création d'une récompense). */
    @Query("""
            select coalesce(sum(s.scoreFinal), 0)
            from SessionOral s
            where s.utilisateur.id = :userId
              and s.etatSession = com.britechnology.edugame.entity.EtatSessionOral.TERMINE
              and coalesce(s.dateDebut, s.dateFin) >= :fromDate
            """)
    Integer sumScoreFinalByUserSince(@Param("userId") Long userId, @Param("fromDate") LocalDateTime fromDate);
}
