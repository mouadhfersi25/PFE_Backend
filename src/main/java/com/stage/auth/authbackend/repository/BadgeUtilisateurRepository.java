package com.stage.auth.authbackend.repository;

import com.stage.auth.authbackend.entity.BadgeUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/**
 * Repository pour badges_utilisateur (liaison User-Badge avec date d'obtention).
 */
public interface BadgeUtilisateurRepository extends JpaRepository<BadgeUtilisateur, Long> {

    @Modifying
    @Query("DELETE FROM BadgeUtilisateur bu WHERE bu.badge.id = :badgeId")
    void deleteByBadgeId(Long badgeId);
}
