package com.britechnology.edugame.repository.game;

import com.britechnology.edugame.entity.ParametresReflexe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParametresReflexeRepository extends JpaRepository<ParametresReflexe, Long> {

    Optional<ParametresReflexe> findByJeuId(Long jeuId);
}

