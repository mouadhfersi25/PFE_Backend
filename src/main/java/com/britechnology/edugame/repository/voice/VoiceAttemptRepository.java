package com.britechnology.edugame.repository.voice;

import com.britechnology.edugame.entity.VoiceAttempt;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VoiceAttemptRepository extends JpaRepository<VoiceAttempt, Long> {
    List<VoiceAttempt> findBySessionOralIdOrderByCreatedAtAsc(Long sessionOralId);

    void deleteByPromptSeriesId(Long seriesId);

    void deleteBySessionOralSeriesId(Long seriesId);
}
