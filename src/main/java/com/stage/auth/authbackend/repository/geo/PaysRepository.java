package com.stage.auth.authbackend.repository.geo;

import com.stage.auth.authbackend.entity.Pays;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PaysRepository extends JpaRepository<Pays, Long> {

    List<Pays> findAllByOrderByNomAsc();

    Optional<Pays> findFirstByNomIgnoreCase(String nom);
}

