package com.britechnology.edugame.service.player;

import com.britechnology.edugame.dto.player.CreateChildRequest;
import com.britechnology.edugame.dto.player.LinkedChildProfileDTO;
import com.britechnology.edugame.dto.player.PlayerBadgeOverviewItemDTO;
import com.britechnology.edugame.dto.player.PlayerHistorySessionDTO;
import com.britechnology.edugame.entity.Badge;
import com.britechnology.edugame.entity.BadgeUtilisateur;
import com.britechnology.edugame.entity.EtatCompte;
import com.britechnology.edugame.entity.EtatSession;
import com.britechnology.edugame.entity.Genre;
import com.britechnology.edugame.entity.TypeJeu;
import com.britechnology.edugame.entity.TypeConditionBadge;
import com.britechnology.edugame.entity.Role;
import com.britechnology.edugame.entity.SessionJeu;
import com.britechnology.edugame.entity.User;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.badge.BadgeRepository;
import com.britechnology.edugame.repository.badge.BadgeUtilisateurRepository;
import com.britechnology.edugame.repository.game.SessionJeuRepository;
import com.britechnology.edugame.repository.user.UserRepository;
import com.britechnology.edugame.service.auth.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ParentLinkageService {

    private final UserRepository userRepository;
    private final SessionJeuRepository sessionJeuRepository;
    private final BadgeRepository badgeRepository;
    private final BadgeUtilisateurRepository badgeUtilisateurRepository;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;

    @Transactional(readOnly = true)
    public List<LinkedChildProfileDTO> getLinkedChildren(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw ApiException.unauthorized("Non authentifié");
        }
        User parent = userRepository.findByEmail(authentication.getName().trim())
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (parent.getRole() != Role.PARENT) {
            throw ApiException.forbidden("Réservé aux comptes parent");
        }
        return userRepository.findByParentIdOrderByPrenomAscNomAsc(parent.getId()).stream()
                .map(this::toLinkedChild)
                .toList();
    }

    @Transactional
    public LinkedChildProfileDTO createChild(Authentication authentication, CreateChildRequest request) {
        User parent = requireParent(authentication);
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
        int age = Period.between(request.getDateDeNaissance(), LocalDate.now()).getYears();
        if (age < 7) {
            throw ApiException.badRequest("L'âge minimum du joueur est de 7 ans");
        }
        if (age > 18) {
            throw ApiException.badRequest("L'âge maximum du joueur est de 18 ans");
        }

        User child = User.builder()
                .nom(request.getNom().trim())
                .prenom(request.getPrenom().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .dateDeNaissance(request.getDateDeNaissance())
                .telephone(request.getTelephone())
                .genre(Genre.parse(request.getGenre()))
                .role(Role.JOUEUR)
                .etatCompte(EtatCompte.ACTIF)
                .enabled(true)
                .parent(parent)
                .niveau(1)
                .scoreTotal(0)
                .pointsExperience(0)
                .currentStreakDays(0)
                .bestStreakDays(0)
                .onboardingCompleted(false)
                .build();
        child = userRepository.save(child);

        String parentDisplayName = ((parent.getPrenom() != null ? parent.getPrenom() : "") + " " +
                (parent.getNom() != null ? parent.getNom() : "")).trim();
        if (parentDisplayName.isEmpty()) {
            parentDisplayName = parent.getEmail();
        }
        emailService.sendChildAccountCreatedEmail(email, request.getPrenom(), request.getPassword(), parentDisplayName);

        return toLinkedChild(child);
    }

    @Transactional(readOnly = true)
    public List<PlayerHistorySessionDTO> getLinkedChildHistory(Authentication authentication, Long childId) {
        User child = resolveLinkedChild(authentication, childId);
        return sessionJeuRepository.findTop120ByUtilisateurIdOrderByDateDebutDesc(child.getId()).stream()
                .map(this::toHistorySessionDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<PlayerBadgeOverviewItemDTO> getLinkedChildBadges(Authentication authentication, Long childId) {
        User child = resolveLinkedChild(authentication, childId);
        List<Badge> badges = badgeRepository.findAllByOrderByNomAsc();
        Map<Long, java.time.LocalDate> earnedByBadgeId = badgeUtilisateurRepository.findByUtilisateurId(child.getId()).stream()
                .filter(link -> link.getBadge() != null && link.getBadge().getId() != null)
                .collect(java.util.stream.Collectors.toMap(
                        link -> link.getBadge().getId(),
                        BadgeUtilisateur::getDateObtention,
                        (a, b) -> a,
                        LinkedHashMap::new
                ));
        return badges.stream()
                .map(badge -> PlayerBadgeOverviewItemDTO.builder()
                        .id(badge.getId())
                        .nom(badge.getNom())
                        .description(badge.getDescription())
                        .icone(badge.getIcone())
                        .unlockCondition(toUnlockConditionLabel(badge))
                        .earned(earnedByBadgeId.containsKey(badge.getId()))
                        .claimable(false)
                        .earnedDate(earnedByBadgeId.get(badge.getId()))
                        .build())
                .toList();
    }

    private LinkedChildProfileDTO toLinkedChild(User child) {
        int level = child.getNiveau() != null ? Math.max(1, child.getNiveau()) : 1;
        return LinkedChildProfileDTO.builder()
                .id(child.getId())
                .nom(child.getNom())
                .prenom(child.getPrenom())
                .email(child.getEmail())
                .dateDeNaissance(child.getDateDeNaissance())
                .avatarUrl(child.getAvatarUrl())
                .niveau(child.getNiveau())
                .scoreTotal(child.getScoreTotal())
                .pointsExperience(child.getPointsExperience())
                .xpToNextLevel(xpToNextLevel(level))
                .currentStreakDays(child.getCurrentStreakDays())
                .bestStreakDays(child.getBestStreakDays())
                .skillMath(averageAccuracy(child.getId(), TypeJeu.QUIZ))
                .skillLogic(averageAccuracy(child.getId(), TypeJeu.LOGIQUE))
                .skillMemory(averageAccuracy(child.getId(), TypeJeu.MEMOIRE))
                .skillReflex(averageAccuracy(child.getId(), TypeJeu.REFLEXE))
                .weeklyPlaytimeMinutes(weeklyPlaytimeMinutes(child.getId()))
                .averageSuccessRate(overallAverageAccuracy(child.getId()))
                .onboardingCompleted(child.isOnboardingCompleted())
                .build();
    }

    private int weeklyPlaytimeMinutes(Long childId) {
        Integer weeklySeconds = sessionJeuRepository.sumDurationSecondsSince(childId, LocalDateTime.now().minusDays(7));
        return Math.max(0, (weeklySeconds != null ? weeklySeconds : 0) / 60);
    }

    private int overallAverageAccuracy(Long childId) {
        Double avg = sessionJeuRepository.averageAccuracyByUser(childId);
        if (avg == null) return 0;
        return Math.max(0, Math.min(100, (int) Math.round(avg)));
    }

    private int averageAccuracy(Long childId, TypeJeu typeJeu) {
        Double avg = sessionJeuRepository.averageAccuracyByUserAndType(childId, typeJeu);
        if (avg == null) return 0;
        int rounded = (int) Math.round(avg);
        return Math.max(0, Math.min(100, rounded));
    }

    private int xpToNextLevel(int level) {
        return Math.max(250, (level * 150) + (level * level * 55));
    }

    private User requireParent(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw ApiException.unauthorized("Non authentifié");
        }
        User parent = userRepository.findByEmail(authentication.getName().trim())
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (parent.getRole() != Role.PARENT) {
            throw ApiException.forbidden("Réservé aux comptes parent");
        }
        return parent;
    }

    private User resolveLinkedChild(Authentication authentication, Long childId) {
        if (childId == null) {
            throw ApiException.badRequest("L'identifiant de l'enfant est requis");
        }
        User parent = requireParent(authentication);
        User child = userRepository.findById(childId)
                .orElseThrow(() -> ApiException.notFound("Enfant introuvable"));
        if (child.getParent() == null || !parent.getId().equals(child.getParent().getId())) {
            throw ApiException.forbidden("Cet enfant n'est pas lié à votre compte");
        }
        return child;
    }

    private String toUnlockConditionLabel(Badge badge) {
        TypeConditionBadge type = badge.getTypeCondition() != null ? badge.getTypeCondition() : TypeConditionBadge.SCORE_MIN;
        int value = badge.getScoreCondition() != null ? Math.max(0, badge.getScoreCondition()) : 0;
        return switch (type) {
            case SCORE_MIN -> "Score total >= " + value;
            case FIRST_WIN -> "Premiere victoire";
            case GAMES_PLAYED -> value + " parties jouees minimum";
            case STREAK_DAYS -> "Streak de " + value + " jours";
            case QUIZ_WIN -> "Victoire en Quiz";
            case PERFECT_GAME -> "Partie parfaite (100% reussite)";
        };
    }

    private PlayerHistorySessionDTO toHistorySessionDTO(SessionJeu session) {
        int score = session.getScoreFinal() != null
                ? session.getScoreFinal()
                : (session.getScoreGlobal() != null ? session.getScoreGlobal() : 0);
        int accuracy = session.getAccuracyPercent() != null ? Math.max(0, session.getAccuracyPercent()) : 0;
        boolean success = session.getEtatSession() == EtatSession.TERMINE && (score > 0 || accuracy >= 60);
        String mode = "EN_LIGNE".equalsIgnoreCase(session.getModeJeuLance()) ? "Online" : "Individual";
        String gameType = session.getJeu() != null && session.getJeu().getTypeJeu() != null
                ? session.getJeu().getTypeJeu().name()
                : "UNKNOWN";
        String status = switch (session.getEtatSession()) {
            case EN_COURS -> "EN_COURS";
            case TERMINE -> "TERMINEE";
            case ABANDONNE -> "ABANDONNEE";
        };
        return PlayerHistorySessionDTO.builder()
                .id(session.getId())
                .gameId(session.getJeu() != null ? session.getJeu().getId() : null)
                .gameTitle(session.getJeu() != null ? session.getJeu().getTitre() : "Jeu")
                .gameType(gameType)
                .dateDebut(session.getDateDebut())
                .dateFin(session.getDateFin())
                .durationSeconds(session.getDurationSeconds())
                .scoreFinal(score)
                .niveauAtteint(session.getNiveauAtteint())
                .reussite(success)
                .statut(status)
                .mode(mode)
                .accuracy(session.getAccuracyPercent())
                .reactionTime(session.getReactionTimeMs())
                .build();
    }
}
