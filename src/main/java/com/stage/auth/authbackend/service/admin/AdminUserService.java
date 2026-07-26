package com.stage.auth.authbackend.service.admin;

import com.stage.auth.authbackend.dto.admin.AdminScoringDistributionDTO;
import com.stage.auth.authbackend.dto.player.UpdateRoleRequest;
import com.stage.auth.authbackend.dto.player.UserDTO;
import com.stage.auth.authbackend.entity.EtatCompte;
import com.stage.auth.authbackend.entity.EtatSession;
import com.stage.auth.authbackend.entity.Role;
import com.stage.auth.authbackend.entity.User;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.game.SessionJeuRepository;
import com.stage.auth.authbackend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final SessionJeuRepository sessionJeuRepository;

    /**
     * Récupère un utilisateur par id (réservé à l'admin). UserDTO = 100 % table users.
     */
    public UserDTO findUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> com.stage.auth.authbackend.exception.ApiException.notFound("Utilisateur introuvable"));
        return toDTO(user);
    }

    /**
     * Récupère tous les utilisateurs (réservé à l'admin).
     * Les champs du UserDTO correspondent à 100 % à la table users.
     */
    public List<UserDTO> findAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Suspendre un utilisateur (état SUSPENDU, enabled = false). Réservé à l'admin.
     */
    public UserDTO suspendUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> com.stage.auth.authbackend.exception.ApiException.notFound("Utilisateur introuvable"));
        user.setEtatCompte(EtatCompte.SUSPENDU);
        user.setEnabled(false);
        user = userRepository.save(user);
        return toDTO(user);
    }

    /**
     * Réactiver un utilisateur (état ACTIF, enabled = true). Réservé à l'admin.
     */
    public UserDTO reactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        user.setEtatCompte(EtatCompte.ACTIF);
        user.setEnabled(true);
        user = userRepository.save(user);
        return toDTO(user);
    }

    private static final Set<Role> ROLES_ALLOWED_TO_ASSIGN = Set.of(Role.JOUEUR, Role.PARENT, Role.EDUCATEUR);

    /**
     * Changer le rôle d'un utilisateur. Réservé à l'admin.
     * Règles : on ne peut pas changer son propre rôle ; on ne peut pas changer le rôle d'un ADMIN.
     */
    public UserDTO updateRole(Long id, UpdateRoleRequest request, String currentAdminEmail) {
        User target = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (target.getEmail() != null && target.getEmail().equalsIgnoreCase(currentAdminEmail)) {
            throw ApiException.badRequest("Vous ne pouvez pas modifier votre propre rôle.");
        }
        if (target.getRole() == Role.ADMIN) {
            throw ApiException.badRequest("Le rôle ADMIN ne peut pas être modifié.");
        }
        String roleStr = request.getRole() != null ? request.getRole().trim().toUpperCase() : "";
        Role newRole;
        try {
            newRole = Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Rôle invalide : " + roleStr);
        }
        if (!ROLES_ALLOWED_TO_ASSIGN.contains(newRole)) {
            throw ApiException.badRequest("Seuls les rôles JOUEUR, PARENT et EDUCATEUR peuvent être attribués.");
        }
        target.setRole(newRole);
        target = userRepository.save(target);
        return toDTO(target);
    }

    public long getActiveSessionsCount() {
        return sessionJeuRepository.countByEtatSession(EtatSession.EN_COURS);
    }

    public List<AdminScoringDistributionDTO> getScoringDistribution() {
        return sessionJeuRepository.fetchScoringDistributionByGame();
    }

    private UserDTO toDTO(User user) {
        return UserDTO.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .password(null) // jamais exposé en API
                .telephone(user.getTelephone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .etatCompte(user.getEtatCompte())
                .enabled(user.isEnabled())
                .dateDeNaissance(user.getDateDeNaissance())
                .niveau(user.getNiveau())
                .scoreTotal(user.getScoreTotal())
                .pointsExperience(user.getPointsExperience())
                .currentStreakDays(user.getCurrentStreakDays())
                .bestStreakDays(user.getBestStreakDays())
                .lastStreakDate(user.getLastStreakDate())
                .idRegion(user.getRegion() != null ? user.getRegion().getId() : null)
                .idPays(user.getRegion() != null && user.getRegion().getPays() != null ? user.getRegion().getPays().getId() : null)
                .onboardingCompleted(user.isOnboardingCompleted())
                .idGenre(user.getGenre() != null ? user.getGenre().getId() : null)
                .resetToken(user.getResetToken())
                .resetTokenExpiry(user.getResetTokenExpiry())
                .tokenVerification(user.getTokenVerification())
                .dateExpirationToken(user.getDateExpirationToken())
                .dateDerniereConnexion(user.getDateDerniereConnexion())
                .dateCreation(user.getDateCreation())
                .build();
    }
}
