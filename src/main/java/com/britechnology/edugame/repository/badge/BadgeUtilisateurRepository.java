package com.britechnology.edugame.repository.badge;

import com.britechnology.edugame.entity.BadgeUtilisateur;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

/**
 * Repository pour badges_utilisateur (liaison User-Badge avec date d'obtention).
 */
public interface BadgeUtilisateurRepository extends JpaRepository<BadgeUtilisateur, Long> {

    List<BadgeUtilisateur> findByUtilisateurId(Long utilisateurId);
    Optional<BadgeUtilisateur> findFirstByUtilisateurIdAndBadgeId(Long utilisateurId, Long badgeId);

    @Modifying
    @Query("DELETE FROM BadgeUtilisateur bu WHERE bu.badge.id = :badgeId")
    void deleteByBadgeId(Long badgeId);
}

