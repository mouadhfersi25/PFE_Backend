package com.stage.auth.authbackend.service.admin;

import com.stage.auth.authbackend.dto.badge.BadgeDTO;
import com.stage.auth.authbackend.dto.badge.CreateBadgeRequest;
import com.stage.auth.authbackend.dto.badge.UpdateBadgeRequest;
import com.stage.auth.authbackend.entity.Badge;
import com.stage.auth.authbackend.entity.TypeConditionBadge;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.badge.BadgeRepository;
import com.stage.auth.authbackend.repository.badge.BadgeUtilisateurRepository;
import com.stage.auth.authbackend.repository.badge.NiveauRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminBadgeService {

    private final BadgeRepository badgeRepository;
    private final BadgeUtilisateurRepository badgeUtilisateurRepository;
    private final NiveauRepository niveauRepository;

    /**
     * Liste tous les badges (ordre par nom).
     */
    public List<BadgeDTO> findAllBadges() {
        return badgeRepository.findAllByOrderByNomAsc().stream()
                .map(AdminBadgeService::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un badge par id.
     */
    public BadgeDTO findBadgeById(Long id) {
        Badge badge = badgeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Badge introuvable"));
        return toDTO(badge);
    }

    /**
     * Crée un nouveau badge.
     */
    @Transactional
    public BadgeDTO createBadge(CreateBadgeRequest request) {
        TypeConditionBadge type = parseTypeCondition(request.getTypeCondition());
        Badge badge = Badge.builder()
                .nom(request.getNom().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .typeCondition(type)
                .scoreCondition(request.getScoreCondition())
                .icone(request.getIcone() != null ? request.getIcone().trim() : null)
                .build();
        badge = badgeRepository.save(badge);
        return toDTO(badge);
    }

    /**
     * Met à jour un badge. Seuls les champs non null du request sont appliqués.
     */
    @Transactional
    public BadgeDTO updateBadge(Long id, UpdateBadgeRequest request) {
        Badge badge = badgeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Badge introuvable"));
        if (request.getNom() != null) badge.setNom(request.getNom().trim());
        if (request.getDescription() != null) badge.setDescription(request.getDescription().trim());
        if (request.getTypeCondition() != null) badge.setTypeCondition(parseTypeCondition(request.getTypeCondition()));
        if (request.getScoreCondition() != null) badge.setScoreCondition(request.getScoreCondition());
        if (request.getIcone() != null) badge.setIcone(request.getIcone().trim().isEmpty() ? null : request.getIcone().trim());
        badge = badgeRepository.save(badge);
        return toDTO(badge);
    }

    /**
     * Supprime un badge. Supprime d'abord les liaisons badges_utilisateur et décroche les niveaux.
     */
    @Transactional
    public void deleteBadge(Long id) {
        Badge badge = badgeRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Badge introuvable"));
        badgeUtilisateurRepository.deleteByBadgeId(id);
        niveauRepository.findByBadgeId(id).forEach(n -> {
            n.setBadge(null);
            niveauRepository.save(n);
        });
        badgeRepository.delete(badge);
    }

    private static TypeConditionBadge parseTypeCondition(String value) {
        if (value == null || value.isBlank()) return TypeConditionBadge.SCORE_MIN;
        try {
            return TypeConditionBadge.valueOf(value.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Type de condition invalide : " + value + ". Valeurs : " + java.util.Arrays.toString(TypeConditionBadge.values()));
        }
    }

    private static BadgeDTO toDTO(Badge b) {
        return BadgeDTO.builder()
                .id(b.getId())
                .nom(b.getNom())
                .description(b.getDescription())
                .typeCondition(b.getTypeCondition() != null ? b.getTypeCondition().name() : TypeConditionBadge.SCORE_MIN.name())
                .scoreCondition(b.getScoreCondition())
                .icone(b.getIcone())
                .build();
    }
}
