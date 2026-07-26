package com.stage.auth.authbackend.repository.game;

import com.stage.auth.authbackend.entity.PuzzleLogique;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PuzzleLogiqueRepository extends JpaRepository<PuzzleLogique, Long> {

    List<PuzzleLogique> findByJeuId(Long jeuId);
}

