package com.stage.auth.authbackend.repository.sponsor;

import com.stage.auth.authbackend.entity.Recompense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecompenseRepository extends JpaRepository<Recompense, Long> {
    List<Recompense> findAllByOrderByIdDesc();
}
