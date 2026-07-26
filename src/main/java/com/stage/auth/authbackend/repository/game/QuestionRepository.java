package com.stage.auth.authbackend.repository.game;

import com.stage.auth.authbackend.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByJeuId(Long jeuId);

    long countByDifficulte(Integer difficulte);
}

