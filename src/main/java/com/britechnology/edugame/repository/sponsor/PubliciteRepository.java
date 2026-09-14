package com.britechnology.edugame.repository.sponsor;

import com.britechnology.edugame.entity.Publicite;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface PubliciteRepository extends JpaRepository<Publicite, Long> {

    List<Publicite> findAllByOrderByIdDesc();

    List<Publicite> findByCreateurIdOrderByIdDesc(Long createurId);

    Optional<Publicite> findByIdAndCreateurId(Long id, Long createurId);

    long countByActiveTrue();

    @Query("""
            SELECT DISTINCT p FROM Publicite p
            JOIN p.jeux j
            WHERE p.active = TRUE AND j.id = :jeuId
            """)
    List<Publicite> findActiveByJeuId(@Param("jeuId") Long jeuId);

    @Query("""
            SELECT DISTINCT p FROM Publicite p
            LEFT JOIN FETCH p.jeux
            LEFT JOIN FETCH p.createur
            WHERE p.id = :id
            """)
    Optional<Publicite> findDetailedById(@Param("id") Long id);

    @Query("""
            SELECT DISTINCT p FROM Publicite p
            LEFT JOIN FETCH p.jeux
            LEFT JOIN FETCH p.createur
            ORDER BY p.id DESC
            """)
    List<Publicite> findAllDetailedOrderByIdDesc();

    @Query("""
            SELECT DISTINCT p FROM Publicite p
            LEFT JOIN FETCH p.jeux
            LEFT JOIN FETCH p.createur
            WHERE p.createur.id = :createurId
            ORDER BY p.id DESC
            """)
    List<Publicite> findDetailedByCreateurIdOrderByIdDesc(@Param("createurId") Long createurId);
}
