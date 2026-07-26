package com.stage.auth.authbackend.repository.user;

import com.stage.auth.authbackend.dto.player.SoloLeaderboardEntryDTO;
import com.stage.auth.authbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long>, JpaSpecificationExecutor<User> {

    List<User> findAll();

    Optional<User> findByEmail(String email);

    Optional<User> findByResetToken(String resetToken);

    Optional<User> findByTokenVerification(String tokenVerification);

    boolean existsByEmail(String email);

    @Query("""
            select new com.stage.auth.authbackend.dto.player.SoloLeaderboardEntryDTO(
                u.id,
                trim(concat(coalesce(u.prenom, ''), ' ', coalesce(u.nom, ''))),
                u.avatarUrl,
                coalesce(u.niveau, 1),
                coalesce(u.scoreTotal, 0)
            )
            from User u
            where u.role = com.stage.auth.authbackend.entity.Role.JOUEUR
            order by coalesce(u.scoreTotal, 0) desc, coalesce(u.pointsExperience, 0) desc, u.id asc
            """)
    List<SoloLeaderboardEntryDTO> fetchSoloLeaderboard();
}

