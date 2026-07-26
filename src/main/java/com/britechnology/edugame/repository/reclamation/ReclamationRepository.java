package com.britechnology.edugame.repository.reclamation;

import com.britechnology.edugame.entity.Reclamation;
import com.britechnology.edugame.entity.StatutReclamation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReclamationRepository extends JpaRepository<Reclamation, Long> {

    boolean existsBySessionJeuId(Long sessionJeuId);

    long countByStatut(StatutReclamation statut);

    List<Reclamation> findByStatutOrderByCreatedAtDesc(StatutReclamation statut);

    List<Reclamation> findAllByOrderByCreatedAtDesc();
}
