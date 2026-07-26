package com.stage.auth.authbackend.repository.badge;

import com.stage.auth.authbackend.entity.Niveau;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour l'entité Niveau (table niveaux).
 */
public interface NiveauRepository extends JpaRepository<Niveau, Long> {

    List<Niveau> findByBadgeId(Long badgeId);
    Optional<Niveau> findByNiveau(Integer niveau);
}

