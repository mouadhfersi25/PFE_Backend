package com.stage.auth.authbackend.repository.game;

import com.stage.auth.authbackend.entity.GameReviewAction;
import com.stage.auth.authbackend.entity.GameReviewHistory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface GameReviewHistoryRepository extends JpaRepository<GameReviewHistory, Long> {
    Optional<GameReviewHistory> findTopByJeuIdOrderByCreatedAtDescIdDesc(Long jeuId);
    Optional<GameReviewHistory> findTopByJeuIdAndActionOrderByCreatedAtDescIdDesc(Long jeuId, GameReviewAction action);
}
