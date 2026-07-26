package com.stage.auth.authbackend.repository;

import com.stage.auth.authbackend.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RegionRepository extends JpaRepository<Region, Long> {
    List<Region> findByPaysIdOrderByNomAsc(Long paysId);
    java.util.Optional<Region> findFirstByPaysIdAndNomIgnoreCase(Long paysId, String nom);
}
