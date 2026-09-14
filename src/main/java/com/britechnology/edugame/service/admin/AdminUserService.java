package com.britechnology.edugame.service.admin;

import com.britechnology.edugame.dto.admin.AdminScoringDistributionDTO;
import com.britechnology.edugame.dto.player.UpdateRoleRequest;
import com.britechnology.edugame.dto.player.UserDTO;
import com.britechnology.edugame.entity.EtatCompte;
import com.britechnology.edugame.entity.EtatSession;
import com.britechnology.edugame.entity.Genre;
import com.britechnology.edugame.entity.Role;
import com.britechnology.edugame.entity.User;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.util.AvatarPolicy;
import com.britechnology.edugame.repository.game.SessionJeuRepository;
import com.britechnology.edugame.repository.user.UserRepository;
import com.britechnology.edugame.service.auth.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminUserService {

    private final UserRepository userRepository;
    private final SessionJeuRepository sessionJeuRepository;
    private final org.springframework.security.crypto.password.PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    /**
     * Récupère un utilisateur par id (réservé à l'admin). UserDTO = 100 % table users.
     */
    @Transactional(readOnly = true)
    public UserDTO findUserById(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        return toDTO(user);
    }

    /**
     * Récupère tous les utilisateurs (réservé à l'admin).
     * Les champs du UserDTO correspondent à 100 % à la table users.
     */
    @Transactional(readOnly = true)
    public List<UserDTO> findAllUsers() {
        List<User> users = userRepository.findAll();
        return users.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Suspendre un utilisateur (état SUSPENDU, enabled = false). Réservé à l'admin.
     */
    @Transactional
    public UserDTO suspendUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        user.setEtatCompte(EtatCompte.SUSPENDU);
        user.setEnabled(false);
        user = userRepository.save(user);

        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            emailService.sendAccountSuspendedEmail(user.getEmail(), user.getPrenom());
        }

        return toDTO(user);
    }

    /**
     * Réactiver un utilisateur (état ACTIF, enabled = true). Réservé à l'admin.
     */
    @Transactional
    public UserDTO reactivateUser(Long id) {
        User user = userRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        user.setEtatCompte(EtatCompte.ACTIF);
        user.setEnabled(true);
        user = userRepository.save(user);
        return toDTO(user);
    }

    private static final Set<Role> ROLES_ALLOWED_TO_ASSIGN = Set.of(Role.JOUEUR, Role.PARENT, Role.EDUCATEUR, Role.SPONSOR);

    /**
     * Changer le rôle d'un utilisateur. Réservé à l'admin.
     * Règles : on ne peut pas changer son propre rôle ; on ne peut pas changer le rôle d'un ADMIN.
     */
    @Transactional
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
            throw ApiException.badRequest("Seuls les rôles JOUEUR, PARENT, EDUCATEUR et SPONSOR peuvent être attribués.");
        }
        target.setRole(newRole);
        if (newRole != Role.JOUEUR) {
            // Gamification (niveau/score/XP/streaks) et avatar : réservés aux comptes JOUEUR.
            target.setAvatarUrl(null);
            target.setNiveau(null);
            target.setScoreTotal(null);
            target.setPointsExperience(null);
            target.setCurrentStreakDays(null);
            target.setBestStreakDays(null);
            target.setLastStreakDate(null);
        } else if (target.getNiveau() == null) {
            target.setNiveau(1);
            target.setScoreTotal(0);
            target.setPointsExperience(0);
            target.setCurrentStreakDays(0);
            target.setBestStreakDays(0);
        }
        target = userRepository.save(target);
        return toDTO(target);
    }

    @Transactional
    public UserDTO createStaffUser(com.britechnology.edugame.dto.admin.CreateStaffUserRequest request) {
        if (request == null) {
            throw ApiException.badRequest("Données manquantes");
        }
        String email = request.getEmail() != null ? request.getEmail().trim().toLowerCase() : "";
        if (email.isEmpty()) {
            throw ApiException.badRequest("L'e-mail est requis");
        }
        if (userRepository.existsByEmail(email)) {
            throw ApiException.badRequest("Cet e-mail est déjà utilisé");
        }
        String roleStr = request.getRole() != null ? request.getRole().trim().toUpperCase() : "";
        Role role;
        try {
            role = Role.valueOf(roleStr);
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Rôle invalide");
        }
        if (role != Role.EDUCATEUR && role != Role.SPONSOR) {
            throw ApiException.badRequest("Seuls les rôles EDUCATEUR et SPONSOR peuvent être créés ici");
        }
        User user = User.builder()
                .nom(request.getNom().trim())
                .prenom(request.getPrenom().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .dateDeNaissance(request.getDateDeNaissance())
                .telephone(request.getTelephone())
                .genre(Genre.parse(request.getGenre()))
                .role(role)
                .etatCompte(EtatCompte.ACTIF)
                .enabled(true)
                .build();
        user = userRepository.save(user);

        String roleLabel = role == Role.EDUCATEUR ? "Éducateur" : "Sponsor";
        emailService.sendStaffAccountCreatedEmail(email, request.getPrenom(), request.getPassword(), roleLabel);

        return toDTO(user);
    }

    /**
     * Rattacher un joueur à un compte PARENT, ou lever le lien (parentId null).
     */
    @Transactional
    public UserDTO setParentLink(Long childUserId, Long parentId) {
        User child = userRepository.findById(childUserId)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (child.getRole() != Role.JOUEUR) {
            throw ApiException.badRequest("Seuls les comptes joueur peuvent être rattachés à un parent.");
        }
        if (parentId == null) {
            child.setParent(null);
            child = userRepository.save(child);
            return toDTO(child);
        }
        if (parentId.equals(childUserId)) {
            throw ApiException.badRequest("Un utilisateur ne peut pas être son propre tuteur.");
        }
        User parent = userRepository.findById(parentId)
                .orElseThrow(() -> ApiException.notFound("Compte parent introuvable"));
        if (parent.getRole() != Role.PARENT) {
            throw ApiException.badRequest("Le tuteur sélectionné doit avoir le rôle PARENT.");
        }
        child.setParent(parent);
        child = userRepository.save(child);
        return toDTO(child);
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
                .cin(user.getCin())
                .avatarUrl(AvatarPolicy.publicAvatarUrl(user))
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
                .genre(user.getGenre() != null ? user.getGenre().name() : null)
                .resetToken(user.getResetToken())
                .resetTokenExpiry(user.getResetTokenExpiry())
                .tokenVerification(user.getTokenVerification())
                .dateExpirationToken(user.getDateExpirationToken())
                .dateDerniereConnexion(user.getDateDerniereConnexion())
                .dateCreation(user.getDateCreation())
                .idParent(user.getParent() != null ? user.getParent().getId() : null)
                .build();
    }
}
