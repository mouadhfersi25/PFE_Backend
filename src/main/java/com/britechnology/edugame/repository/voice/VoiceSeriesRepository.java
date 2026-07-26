package com.britechnology.edugame.repository.voice;

import com.britechnology.edugame.entity.EtatVoiceSeries;
import com.britechnology.edugame.entity.VoiceSeries;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoiceSeriesRepository extends JpaRepository<VoiceSeries, Long> {
    List<VoiceSeries> findByEducateurIdOrderByUpdatedAtDesc(Long educateurId);

    List<VoiceSeries> findByEtatOrderByPublishedAtDesc(EtatVoiceSeries etat);
}
