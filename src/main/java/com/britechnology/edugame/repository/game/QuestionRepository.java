package com.britechnology.edugame.repository.game;

import com.britechnology.edugame.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByJeuId(Long jeuId);

    long countByDifficulte(Integer difficulte);
}

