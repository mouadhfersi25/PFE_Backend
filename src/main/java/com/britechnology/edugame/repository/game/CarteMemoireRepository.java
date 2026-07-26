package com.britechnology.edugame.repository.game;

import com.britechnology.edugame.entity.CarteMemoire;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CarteMemoireRepository extends JpaRepository<CarteMemoire, Long> {

    List<CarteMemoire> findByJeuId(Long jeuId);
}

