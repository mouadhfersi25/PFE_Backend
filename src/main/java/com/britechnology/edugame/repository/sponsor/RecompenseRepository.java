package com.britechnology.edugame.repository.sponsor;

import com.britechnology.edugame.entity.Recompense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RecompenseRepository extends JpaRepository<Recompense, Long> {
    List<Recompense> findAllByOrderByIdDesc();
}
