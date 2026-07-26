package com.stage.auth.authbackend.repository.badge;

import com.stage.auth.authbackend.entity.Badge;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * Repository pour l'entité Badge (table badges).
 */
public interface BadgeRepository extends JpaRepository<Badge, Long> {

    List<Badge> findAllByOrderByNomAsc();
}

