package com.britechnology.edugame.repository.reward;

import com.britechnology.edugame.entity.DemandeRecompense;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface DemandeRecompenseRepository extends JpaRepository<DemandeRecompense, Long> {
    List<DemandeRecompense> findByUtilisateurIdOrderByDateDemandeDesc(Long utilisateurId);
    Optional<DemandeRecompense> findFirstByUtilisateurIdAndRecompenseIdOrderByDateDemandeDesc(Long utilisateurId, Long recompenseId);
    List<DemandeRecompense> findAllByOrderByDateDemandeDescIdDesc();
}
