package com.britechnology.edugame.repository.voice;

import com.britechnology.edugame.entity.SessionOral;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SessionOralRepository extends JpaRepository<SessionOral, Long> {
    List<SessionOral> findTop50ByUtilisateurIdOrderByDateDebutDesc(Long utilisateurId);

    long countByUtilisateurIdAndSeriesIdAndDateDebutAfter(Long utilisateurId, Long seriesId, LocalDateTime after);

    void deleteBySeriesId(Long seriesId);
}
