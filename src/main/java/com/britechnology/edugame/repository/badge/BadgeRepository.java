package com.britechnology.edugame.repository.badge;

import com.britechnology.edugame.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository pour l'entité Badge (table badges).
 */
public interface BadgeRepository extends JpaRepository<Badge, Long> {

    List<Badge> findAllByOrderByNomAsc();
}

