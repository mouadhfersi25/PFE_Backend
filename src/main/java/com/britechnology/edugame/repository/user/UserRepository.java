package com.britechnology.edugame.repository.user;

import com.britechnology.edugame.dto.player.SoloLeaderboardEntryDTO;
import com.britechnology.edugame.entity.User;
import com.britechnology.edugame.entity.Role;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    List<User> findAll();

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    Optional<User> findByTokenVerification(String tokenVerification);

    boolean existsByEmail(String email);

    @Query("""
            select new com.britechnology.edugame.dto.player.SoloLeaderboardEntryDTO(
                u.id,
                trim(concat(coalesce(u.prenom, ''), ' ', coalesce(u.nom, ''))),
                u.avatarUrl,
                coalesce(u.niveau, 1),
                coalesce(u.scoreTotal, 0)
            )
            from User u
            where u.role = com.britechnology.edugame.entity.Role.JOUEUR
            order by coalesce(u.scoreTotal, 0) desc, coalesce(u.pointsExperience, 0) desc, u.id asc
            """)
    List<SoloLeaderboardEntryDTO> fetchSoloLeaderboard();

    @Query("""
            select new com.britechnology.edugame.dto.player.SoloLeaderboardEntryDTO(
                u.id,
                trim(concat(coalesce(u.prenom, ''), ' ', coalesce(u.nom, ''))),
                u.avatarUrl,
                coalesce(u.niveau, 1),
                coalesce(u.scoreTotal, 0)
            )
            from User u
            join u.region r
            join r.pays p
            where u.role = com.britechnology.edugame.entity.Role.JOUEUR
              and u.onboardingCompleted = true
              and p.id = :paysId
            order by coalesce(u.scoreTotal, 0) desc, coalesce(u.pointsExperience, 0) desc, u.id asc
            """)
    List<SoloLeaderboardEntryDTO> fetchSoloLeaderboardByPaysId(@Param("paysId") Long paysId);

    @Query("""
            select new com.britechnology.edugame.dto.player.SoloLeaderboardEntryDTO(
                u.id,
                trim(concat(coalesce(u.prenom, ''), ' ', coalesce(u.nom, ''))),
                u.avatarUrl,
                coalesce(u.niveau, 1),
                coalesce(u.scoreTotal, 0)
            )
            from User u
            join u.region r
            where u.role = com.britechnology.edugame.entity.Role.JOUEUR
              and u.onboardingCompleted = true
              and r.id = :regionId
            order by coalesce(u.scoreTotal, 0) desc, coalesce(u.pointsExperience, 0) desc, u.id asc
            """)
    List<SoloLeaderboardEntryDTO> fetchSoloLeaderboardByRegionId(@Param("regionId") Long regionId);

    @Query("""
            select count(u) + 1
            from User u
            where u.role = com.britechnology.edugame.entity.Role.JOUEUR
              and (
                coalesce(u.scoreTotal, 0) > :score
                or (coalesce(u.scoreTotal, 0) = :score and coalesce(u.pointsExperience, 0) > :xp)
                or (coalesce(u.scoreTotal, 0) = :score and coalesce(u.pointsExperience, 0) = :xp and u.id < :userId)
              )
            """)
    int countPlayersAheadGlobal(@Param("score") int score, @Param("xp") int xp, @Param("userId") Long userId);

    @Query("""
            select count(u) + 1
            from User u
            join u.region r
            join r.pays p
            where u.role = com.britechnology.edugame.entity.Role.JOUEUR
              and u.onboardingCompleted = true
              and p.id = :paysId
              and (
                coalesce(u.scoreTotal, 0) > :score
                or (coalesce(u.scoreTotal, 0) = :score and coalesce(u.pointsExperience, 0) > :xp)
                or (coalesce(u.scoreTotal, 0) = :score and coalesce(u.pointsExperience, 0) = :xp and u.id < :userId)
              )
            """)
    int countPlayersAheadInPays(
            @Param("paysId") Long paysId,
            @Param("score") int score,
            @Param("xp") int xp,
            @Param("userId") Long userId
    );

    @Query("""
            select count(u) + 1
            from User u
            join u.region r
            where u.role = com.britechnology.edugame.entity.Role.JOUEUR
              and u.onboardingCompleted = true
              and r.id = :regionId
              and (
                coalesce(u.scoreTotal, 0) > :score
                or (coalesce(u.scoreTotal, 0) = :score and coalesce(u.pointsExperience, 0) > :xp)
                or (coalesce(u.scoreTotal, 0) = :score and coalesce(u.pointsExperience, 0) = :xp and u.id < :userId)
              )
            """)
    int countPlayersAheadInRegion(
            @Param("regionId") Long regionId,
            @Param("score") int score,
            @Param("xp") int xp,
            @Param("userId") Long userId
    );

    @Query("select count(u) from User u where u.role = com.britechnology.edugame.entity.Role.JOUEUR")
    long countSoloPlayersGlobal();

    @Query("""
            select count(u) from User u
            join u.region r
            join r.pays p
            where u.role = com.britechnology.edugame.entity.Role.JOUEUR
              and u.onboardingCompleted = true
              and p.id = :paysId
            """)
    long countSoloPlayersByPaysId(@Param("paysId") Long paysId);

    @Query("""
            select count(u) from User u
            join u.region r
            where u.role = com.britechnology.edugame.entity.Role.JOUEUR
              and u.onboardingCompleted = true
              and r.id = :regionId
            """)
    long countSoloPlayersByRegionId(@Param("regionId") Long regionId);

    List<User> findByParentIdOrderByPrenomAscNomAsc(Long parentId);

    List<User> findByRoleOrderByPrenomAscNomAsc(Role role);
}

