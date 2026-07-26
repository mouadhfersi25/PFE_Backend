package com.britechnology.edugame.repository.voice;

import com.britechnology.edugame.entity.VoicePrompt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoicePromptRepository extends JpaRepository<VoicePrompt, Long> {
    List<VoicePrompt> findBySeriesIdOrderByOrdreAscIdAsc(Long seriesId);

    long countBySeriesId(Long seriesId);

    void deleteBySeriesId(Long seriesId);
}
