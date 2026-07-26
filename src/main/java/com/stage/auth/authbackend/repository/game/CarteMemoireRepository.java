package com.stage.auth.authbackend.repository.game;

import com.stage.auth.authbackend.entity.CarteMemoire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarteMemoireRepository extends JpaRepository<CarteMemoire, Long> {

    List<CarteMemoire> findByJeuId(Long jeuId);
}

