package com.britechnology.edugame.service.player;

import com.britechnology.edugame.dto.player.LinkedChildProfileDTO;
import com.britechnology.edugame.dto.player.PlayerBadgeOverviewItemDTO;
import com.britechnology.edugame.dto.player.PlayerHistorySessionDTO;
import com.britechnology.edugame.entity.Badge;
import com.britechnology.edugame.entity.BadgeUtilisateur;
import com.britechnology.edugame.entity.EtatSession;
import com.britechnology.edugame.entity.TypeJeu;
import com.britechnology.edugame.entity.TypeConditionBadge;
import com.britechnology.edugame.entity.Role;
import com.britechnology.edugame.entity.SessionJeu;
import com.britechnology.edugame.entity.User;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.badge.BadgeRepository;
import com.britechnology.edugame.repository.badge.BadgeUtilisateurRepository;
import com.britechnology.edugame.repository.badge.NiveauRepository;
import com.britechnology.edugame.repository.game.SessionJeuRepository;
import com.britechnology.edugame.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ParentLinkageService {

    private final UserRepository userRepository;
    private final NiveauRepository niveauRepository;
    private final SessionJeuRepository sessionJeuRepository;
    private final BadgeRepository badgeRepository;
    private final BadgeUtilisateurRepository badgeUtilisateurRepository;

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
                .onboardingCompleted(child.isOnboardingCompleted())
                .build();
    }

    private int averageAccuracy(Long childId, TypeJeu typeJeu) {
        Double avg = sessionJeuRepository.averageAccuracyByUserAndType(childId, typeJeu);
        if (avg == null) return 0;
        int rounded = (int) Math.round(avg);
        return Math.max(0, Math.min(100, rounded));
    }

    private int xpToNextLevel(int level) {
        return niveauRepository.findByNiveau(level)
                .map(cfg -> Math.max(1, cfg.getPointMin() != null ? cfg.getPointMin() : 0))
                .orElse(Math.max(250, (level * 150) + (level * level * 55)));
    }

    private User resolveLinkedChild(Authentication authentication, Long childId) {
        if (authentication == null || authentication.getName() == null) {
            throw ApiException.unauthorized("Non authentifié");
        }
        if (childId == null) {
            throw ApiException.badRequest("childId est requis");
        }
        User parent = userRepository.findByEmail(authentication.getName().trim())
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (parent.getRole() != Role.PARENT) {
            throw ApiException.forbidden("Réservé aux comptes parent");
        }
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
