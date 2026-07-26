package com.britechnology.edugame.repository.game;

import com.britechnology.edugame.entity.PuzzleLogique;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PuzzleLogiqueRepository extends JpaRepository<PuzzleLogique, Long> {

    List<PuzzleLogique> findByJeuId(Long jeuId);
}

