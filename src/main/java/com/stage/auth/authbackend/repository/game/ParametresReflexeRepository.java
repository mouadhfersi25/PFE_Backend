package com.stage.auth.authbackend.repository.game;

import com.stage.auth.authbackend.entity.ParametresReflexe;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ParametresReflexeRepository extends JpaRepository<ParametresReflexe, Long> {

    Optional<ParametresReflexe> findByJeuId(Long jeuId);
}

