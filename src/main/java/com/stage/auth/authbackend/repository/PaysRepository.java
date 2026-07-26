package com.stage.auth.authbackend.repository;

import com.stage.auth.authbackend.entity.Pays;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PaysRepository extends JpaRepository<Pays, Long> {
    List<Pays> findAllByOrderByNomAsc();
    java.util.Optional<Pays> findFirstByNomIgnoreCase(String nom);
}
