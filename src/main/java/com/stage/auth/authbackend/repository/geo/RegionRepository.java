package com.stage.auth.authbackend.repository.geo;

import com.stage.auth.authbackend.entity.Region;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RegionRepository extends JpaRepository<Region, Long> {

    List<Region> findByPaysIdOrderByNomAsc(Long paysId);

    Optional<Region> findFirstByPaysIdAndNomIgnoreCase(Long paysId, String nom);
}

