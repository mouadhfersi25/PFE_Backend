package com.britechnology.edugame.repository.game;

import com.britechnology.edugame.entity.GameReviewAction;
import com.britechnology.edugame.entity.GameReviewHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameReviewHistoryRepository extends JpaRepository<GameReviewHistory, Long> {
    Optional<GameReviewHistory> findTopByJeuIdOrderByCreatedAtDescIdDesc(Long jeuId);
    Optional<GameReviewHistory> findTopByJeuIdAndActionOrderByCreatedAtDescIdDesc(Long jeuId, GameReviewAction action);
}
