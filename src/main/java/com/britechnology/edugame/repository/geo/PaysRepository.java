package com.britechnology.edugame.repository.geo;

import com.britechnology.edugame.entity.Pays;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaysRepository extends JpaRepository<Pays, Long> {

    List<Pays> findAllByOrderByNomAsc();

    Optional<Pays> findFirstByNomIgnoreCase(String nom);
}

