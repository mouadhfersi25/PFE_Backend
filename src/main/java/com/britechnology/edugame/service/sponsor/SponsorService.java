package com.britechnology.edugame.service.sponsor;

import com.britechnology.edugame.dto.game.GameDTO;
import com.britechnology.edugame.dto.player.PlayerAdDTO;
import com.britechnology.edugame.dto.player.RecordAdInteractionRequest;
import com.britechnology.edugame.dto.sponsor.*;
import com.britechnology.edugame.entity.*;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.game.JeuRepository;
import com.britechnology.edugame.repository.reward.DemandeRecompenseRepository;
import com.britechnology.edugame.repository.sponsor.InteractionPubliciteRepository;
import com.britechnology.edugame.repository.sponsor.PubliciteRepository;
import com.britechnology.edugame.repository.sponsor.RecompenseRepository;
import com.britechnology.edugame.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
@Service
@RequiredArgsConstructor
public class SponsorService {

    private final ExternalAdsClient externalAdsClient;
    private final UserRepository userRepository;
    private final RecompenseRepository recompenseRepository;
    private final DemandeRecompenseRepository demandeRecompenseRepository;
    private final PubliciteRepository publiciteRepository;
    private final InteractionPubliciteRepository interactionPubliciteRepository;
    private final JeuRepository jeuRepository;

    public SponsorDashboardStatsDTO getDashboardStats(Authentication authentication) {
        User user = ensureSponsorAccess(authentication);
        List<Publicite> ads = isAdmin(user)
                ? publiciteRepository.findAll()
                : publiciteRepository.findByCreateurIdOrderByIdDesc(user.getId());

        int totalCampaigns = ads.size();
        int activeCampaigns = (int) ads.stream().filter(ad -> Boolean.TRUE.equals(ad.getActive())).count();
        int pausedCampaigns = Math.max(0, totalCampaigns - activeCampaigns);
        int totalImpressions = ads.stream().mapToInt(ad -> ad.getNbVues() == null ? 0 : ad.getNbVues()).sum();
        int totalClicks = ads.stream().mapToInt(ad -> ad.getNbClics() == null ? 0 : ad.getNbClics()).sum();

        int rewardStock = (int) recompenseRepository.countByActiveTrue();
        int distributedRewards = (int) demandeRecompenseRepository.countByStatutIgnoreCase("APPROVED");
        int pendingRewardRequests = (int) demandeRecompenseRepository.countByStatutIgnoreCase("PENDING");

        return SponsorDashboardStatsDTO.builder()
                .totalCampaigns(totalCampaigns)
                .activeCampaigns(activeCampaigns)
                .pausedCampaigns(pausedCampaigns)
                .totalImpressions(totalImpressions)
                .totalClicks(totalClicks)
                .distributedRewards(distributedRewards)
                .rewardStock(rewardStock)
                .pendingRewardRequests(pendingRewardRequests)
                .build();
    }

    @Transactional(readOnly = true)
    public List<PubliciteDTO> listPublicites(Authentication authentication) {
        User user = ensureSponsorAccess(authentication);
        List<Publicite> rows = isAdmin(user)
                ? publiciteRepository.findAllDetailedOrderByIdDesc()
                : publiciteRepository.findDetailedByCreateurIdOrderByIdDesc(user.getId());
        return rows.stream().map(this::toPubliciteDTO).toList();
    }

    @Transactional(readOnly = true)
    public PubliciteDTO getPubliciteById(Authentication authentication, Long id) {
        return toPubliciteDTO(resolveOwnedPublicite(authentication, id));
    }

    @Transactional
    public PubliciteDTO createPublicite(Authentication authentication, CreatePubliciteRequest request) {
        User user = ensureSponsorAccess(authentication);
        if (request == null) {
            throw ApiException.badRequest("Le corps de la requête est requis");
        }
        String contenu = requireText(request.getContenu(), "Le contenu est obligatoire");
        String videoUrl = resolveVideoUrl(request.getVideoUrl(), request.getImageUrl());
        validateVideoUrl(videoUrl);
        int duration = normalizeDuration(request.getAdDurationSeconds());
        Set<Jeu> jeux = resolveTargetGames(request.getJeuIds());

        Publicite publicite = Publicite.builder()
                .contenu(contenu)
                .videoUrl(videoUrl)
                .typePublicite("VIDEO")
                .adDurationSeconds(duration)
                .ctaLabel(trimToNull(request.getCtaLabel()))
                .ctaUrl(requireText(request.getCtaUrl(), "L'URL de l'offre (CTA) est obligatoire"))
                .nbVues(0)
                .nbClics(0)
                .active(true)
                .createur(user)
                .jeux(jeux)
                .build();

        return toPubliciteDTO(publiciteRepository.save(publicite));
    }

    @Transactional
    public PubliciteDTO updatePublicite(Authentication authentication, Long id, UpdatePubliciteRequest request) {
        Publicite publicite = resolveOwnedPublicite(authentication, id);
        if (request == null) {
            throw ApiException.badRequest("Le corps de la requête est requis");
        }
        if (request.getContenu() != null && !request.getContenu().isBlank()) {
            publicite.setContenu(request.getContenu().trim());
        }
        String videoUrl = firstNonBlank(request.getVideoUrl(), request.getImageUrl());
        if (videoUrl != null) {
            validateVideoUrl(videoUrl);
            publicite.setVideoUrl(videoUrl.trim());
        }
        if (request.getAdDurationSeconds() != null) {
            publicite.setAdDurationSeconds(normalizeDuration(request.getAdDurationSeconds()));
        }
        if (request.getCtaLabel() != null) {
            publicite.setCtaLabel(trimToNull(request.getCtaLabel()));
        }
        if (request.getCtaUrl() != null && !request.getCtaUrl().isBlank()) {
            publicite.setCtaUrl(request.getCtaUrl().trim());
        }
        if (request.getJeuIds() != null) {
            publicite.setJeux(resolveTargetGames(request.getJeuIds()));
        }
        return toPubliciteDTO(publiciteRepository.save(publicite));
    }

    @Transactional
    public PubliciteDTO setPubliciteStatus(Authentication authentication, Long id, boolean active) {
        Publicite publicite = resolveOwnedPublicite(authentication, id);
        publicite.setActive(active);
        return toPubliciteDTO(publiciteRepository.save(publicite));
    }

    @Transactional
    public void deletePublicite(Authentication authentication, Long id) {
        Publicite publicite = resolveOwnedPublicite(authentication, id);
        publiciteRepository.delete(publicite);
    }

    @Transactional(readOnly = true)
    public List<GameDTO> listAvailableGamesForAds(Authentication authentication) {
        ensureSponsorAccess(authentication);
        return jeuRepository.findAll().stream()
                .filter(j -> j.getEtat() == EtatJeu.ACCEPTE && j.isActif())
                .map(this::toGameDTO)
                .toList();
    }

    @Transactional(readOnly = true)
    public PlayerAdDTO getActiveAdForGame(Authentication authentication, Long jeuId) {
        ensurePlayerAccess(authentication);
        if (jeuId == null) {
            throw ApiException.badRequest("L'identifiant du jeu est requis");
        }
        if (!jeuRepository.existsById(jeuId)) {
            throw ApiException.notFound("Jeu introuvable");
        }
        List<Publicite> candidates = publiciteRepository.findActiveByJeuId(jeuId);
        if (candidates.isEmpty()) {
            return null;
        }
        Publicite selected = candidates.get(ThreadLocalRandom.current().nextInt(candidates.size()));
        return toPlayerAdDTO(selected);
    }

    @Transactional
    public void recordAdInteraction(Authentication authentication, Long publiciteId, RecordAdInteractionRequest request) {
        User user = ensurePlayerAccess(authentication);
        if (publiciteId == null) {
            throw ApiException.badRequest("id publicité requis");
        }
        if (request == null || request.getTypeInteraction() == null || request.getTypeInteraction().isBlank()) {
            throw ApiException.badRequest("Le type d'interaction est requis (vue ou clic)");
        }
        String type = request.getTypeInteraction().trim().toUpperCase(Locale.ROOT);
        if (!"VIEW".equals(type) && !"CLICK".equals(type)) {
            throw ApiException.badRequest("Type d'interaction invalide. Valeurs acceptées : VIEW, CLICK");
        }
        Publicite publicite = publiciteRepository.findById(publiciteId)
                .orElseThrow(() -> ApiException.notFound("Publicité introuvable"));
        if (!Boolean.TRUE.equals(publicite.getActive())) {
            throw ApiException.badRequest("Cette publicité n'est plus active");
        }

        interactionPubliciteRepository.save(InteractionPublicite.builder()
                .publicite(publicite)
                .utilisateur(user)
                .sessionId(request.getSessionId())
                .typeInteraction(type)
                .build());

        if ("VIEW".equals(type)) {
            publicite.setNbVues((publicite.getNbVues() == null ? 0 : publicite.getNbVues()) + 1);
        } else {
            publicite.setNbClics((publicite.getNbClics() == null ? 0 : publicite.getNbClics()) + 1);
        }
        publiciteRepository.save(publicite);
    }

    @Transactional(readOnly = true)
    public List<RecompenseDTO> listRecompenses(Authentication authentication) {
        User user = ensureSponsorAccess(authentication);
        List<Recompense> localRewards = isAdmin(user)
                ? recompenseRepository.findAllByOrderByIdDesc()
                : recompenseRepository.findBySponsorIdOrderByIdDesc(user.getId());
        if (!localRewards.isEmpty() || recompenseRepository.count() > 0) {
            return localRewards.stream().map(this::toRecompenseDTOLocal).toList();
        }
        if (!externalAdsClient.isEnabled()) return List.of();
        var payload = externalAdsClient.get("/rewards");
        if (payload == null || !payload.isArray()) return List.of();
        List<RecompenseDTO> externalRewards = java.util.stream.StreamSupport.stream(payload.spliterator(), false)
                .map(this::toRecompenseDTOFromExternalFallbackActive)
                .toList();
        if (externalRewards.isEmpty()) return List.of();
        List<Recompense> imported = externalRewards.stream()
                .map(dto -> Recompense.builder()
                        .nom(dto.getNom() == null ? "Récompense" : dto.getNom())
                        .description(dto.getDescription())
                        .scoreMin(dto.getScoreMin())
                        .typeRecompense(parseTypeRecompenseOrDefault(dto.getTypeRecompense()))
                        .dateCreation(LocalDate.now())
                        .active(!"INACTIVE".equalsIgnoreCase(dto.getStatus()))
                        .build())
                .toList();
        return recompenseRepository.saveAll(imported).stream()
                .map(this::toRecompenseDTOLocal)
                .toList();
    }

    @Transactional(readOnly = true)
    public RecompenseDTO getRecompenseById(Authentication authentication, Long id) {
        return toRecompenseDTOLocal(resolveOwnedRecompense(authentication, id));
    }

    public List<SponsorRewardRequestDTO> listRewardRequests(Authentication authentication) {
        ensureSponsorAccess(authentication);
        return demandeRecompenseRepository.findAllByOrderByDateDemandeDescIdDesc().stream()
                .map(this::toSponsorRewardRequestDTO)
                .toList();
    }

    public SponsorRewardRequestDTO updateRewardRequestStatus(Authentication authentication, Long requestId, String status) {
        ensureSponsorAccess(authentication);
        if (requestId == null) throw ApiException.badRequest("L'identifiant de la demande est requis");
        String normalized = normalizeRewardRequestStatus(status);
        DemandeRecompense request = demandeRecompenseRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Demande de récompense introuvable"));
        request.setStatut(normalized);
        return toSponsorRewardRequestDTO(demandeRecompenseRepository.save(request));
    }

    @Transactional
    public RecompenseDTO createRecompense(Authentication authentication, CreateRecompenseRequest request) {
        User user = ensureSponsorAccess(authentication);
        if (request == null || request.getNom() == null || request.getNom().isBlank()) {
            throw ApiException.badRequest("Le nom de la récompense est requis");
        }
        if (request.getTypeRecompense() == null || request.getTypeRecompense().isBlank()) {
            throw ApiException.badRequest("Le type de récompense est requis");
        }
        if (request.getScoreMin() == null || request.getScoreMin() < 0) {
            throw ApiException.badRequest("Le score minimum doit être un entier positif");
        }
        Recompense reward = Recompense.builder()
                .nom(request.getNom().trim())
                .description(request.getDescription())
                .scoreMin(request.getScoreMin())
                .typeRecompense(parseTypeRecompense(request.getTypeRecompense()))
                .dateCreation(LocalDate.now())
                .active(true)
                .sponsor(user)
                .build();
        Recompense saved = recompenseRepository.save(reward);
        if (externalAdsClient.isEnabled()) {
            externalAdsClient.post("/rewards", toRewardPayload(request));
        }
        return toRecompenseDTOLocal(saved);
    }

    @Transactional
    public RecompenseDTO updateRecompense(Authentication authentication, Long id, UpdateRecompenseRequest request) {
        Recompense reward = resolveOwnedRecompense(authentication, id);
        if (request == null) {
            throw ApiException.badRequest("Le corps de la requête est requis");
        }
        if (request.getNom() != null && !request.getNom().isBlank()) {
            reward.setNom(request.getNom().trim());
        }
        if (request.getDescription() != null) {
            reward.setDescription(request.getDescription().trim());
        }
        if (request.getScoreMin() != null) {
            if (request.getScoreMin() < 0) {
                throw ApiException.badRequest("Le score minimum doit être un entier positif");
            }
            reward.setScoreMin(request.getScoreMin());
        }
        if (request.getTypeRecompense() != null && !request.getTypeRecompense().isBlank()) {
            reward.setTypeRecompense(parseTypeRecompense(request.getTypeRecompense()));
        }
        Recompense saved = recompenseRepository.save(reward);
        if (externalAdsClient.isEnabled()) {
            externalAdsClient.put("/rewards/" + id, toRewardPayload(request));
        }
        return toRecompenseDTOLocal(saved);
    }

    @Transactional
    public RecompenseDTO setRecompenseStatus(Authentication authentication, Long id, boolean active) {
        Recompense reward = resolveOwnedRecompense(authentication, id);
        reward.setActive(active);
        Recompense saved = recompenseRepository.save(reward);
        if (externalAdsClient.isEnabled()) {
            externalAdsClient.patch("/rewards/" + id + "/status", Map.of("active", active));
        }
        return toRecompenseDTOLocal(saved);
    }

    @Transactional
    public void deleteRecompense(Authentication authentication, Long id) {
        Recompense reward = resolveOwnedRecompense(authentication, id);
        recompenseRepository.delete(reward);
        if (externalAdsClient.isEnabled()) {
            externalAdsClient.delete("/rewards/" + id);
        }
    }

    private Publicite resolveOwnedPublicite(Authentication authentication, Long id) {
        User user = ensureSponsorAccess(authentication);
        if (id == null) throw ApiException.badRequest("id est requis");
        Publicite publicite = publiciteRepository.findDetailedById(id)
                .orElseThrow(() -> ApiException.notFound("Publicité introuvable"));
        if (!isAdmin(user) && (publicite.getCreateur() == null || !user.getId().equals(publicite.getCreateur().getId()))) {
            throw ApiException.forbidden("Vous ne pouvez gérer que vos propres publicités");
        }
        return publicite;
    }

    private Recompense resolveOwnedRecompense(Authentication authentication, Long id) {
        User user = ensureSponsorAccess(authentication);
        if (id == null) throw ApiException.badRequest("id est requis");
        Recompense reward = recompenseRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Récompense introuvable"));
        if (!isAdmin(user) && (reward.getSponsor() == null || !user.getId().equals(reward.getSponsor().getId()))) {
            throw ApiException.forbidden("Vous ne pouvez gérer que vos propres récompenses");
        }
        return reward;
    }

    private Set<Jeu> resolveTargetGames(List<Long> jeuIds) {
        if (jeuIds == null || jeuIds.isEmpty()) {
            throw ApiException.badRequest("Sélectionnez au moins un jeu cible");
        }
        List<Long> distinctIds = jeuIds.stream().filter(id -> id != null && id > 0).distinct().toList();
        if (distinctIds.isEmpty()) {
            throw ApiException.badRequest("Sélectionnez au moins un jeu cible");
        }
        List<Jeu> found = jeuRepository.findAllById(distinctIds);
        if (found.size() != distinctIds.size()) {
            throw ApiException.badRequest("Un ou plusieurs jeux sélectionnés sont introuvables");
        }
        for (Jeu jeu : found) {
            if (jeu.getEtat() != EtatJeu.ACCEPTE || !jeu.isActif()) {
                throw ApiException.badRequest("Le jeu \"" + jeu.getTitre() + "\" n'est pas disponible pour les publicités");
            }
        }
        return new HashSet<>(found);
    }

    private String resolveVideoUrl(String videoUrl, String imageUrl) {
        return requireText(firstNonBlank(videoUrl, imageUrl), "L'URL vidéo est obligatoire");
    }

    private void validateVideoUrl(String videoUrl) {
        String normalized = videoUrl.trim().toLowerCase(Locale.ROOT);
        if (!(normalized.contains(".mp4") || normalized.contains(".webm") || normalized.contains(".ogg"))) {
            throw ApiException.badRequest("L'URL vidéo doit pointer vers un fichier .mp4, .webm ou .ogg");
        }
    }

    private int normalizeDuration(Integer raw) {
        int duration = raw == null ? 8 : raw;
        if (duration < 3 || duration > 60) {
            throw ApiException.badRequest("La durée pub doit être entre 3 et 60 secondes");
        }
        return duration;
    }

    private PubliciteDTO toPubliciteDTO(Publicite p) {
        List<Jeu> jeux = p.getJeux() == null ? List.of() : p.getJeux().stream().toList();
        String creatorName = null;
        if (p.getCreateur() != null) {
            creatorName = ((p.getCreateur().getPrenom() != null ? p.getCreateur().getPrenom() : "")
                    + " " + (p.getCreateur().getNom() != null ? p.getCreateur().getNom() : "")).trim();
            if (creatorName.isBlank()) {
                creatorName = p.getCreateur().getEmail();
            }
        }
        return PubliciteDTO.builder()
                .id(p.getId())
                .contenu(p.getContenu())
                .status(Boolean.TRUE.equals(p.getActive()) ? "ACTIVE" : "PAUSED")
                .typePublicite(p.getTypePublicite())
                .imageUrl(p.getVideoUrl())
                .videoUrl(p.getVideoUrl())
                .adDurationSeconds(p.getAdDurationSeconds())
                .ctaLabel(p.getCtaLabel())
                .ctaUrl(p.getCtaUrl())
                .nbVues(p.getNbVues())
                .nbClics(p.getNbClics())
                .sponsorId(p.getCreateur() != null ? p.getCreateur().getId() : null)
                .sponsorNom(creatorName)
                .sponsorEmail(p.getCreateur() != null ? p.getCreateur().getEmail() : null)
                .jeuIds(jeux.stream().map(Jeu::getId).toList())
                .jeuTitres(jeux.stream().map(Jeu::getTitre).toList())
                .build();
    }

    private PlayerAdDTO toPlayerAdDTO(Publicite p) {
        String sponsorNom = null;
        if (p.getCreateur() != null) {
            sponsorNom = ((p.getCreateur().getPrenom() != null ? p.getCreateur().getPrenom() : "")
                    + " " + (p.getCreateur().getNom() != null ? p.getCreateur().getNom() : "")).trim();
            if (sponsorNom.isBlank()) {
                sponsorNom = p.getCreateur().getEmail();
            }
        }
        return PlayerAdDTO.builder()
                .id(p.getId())
                .contenu(p.getContenu())
                .videoUrl(p.getVideoUrl())
                .adDurationSeconds(p.getAdDurationSeconds())
                .ctaLabel(p.getCtaLabel() == null || p.getCtaLabel().isBlank() ? "Voir l'offre" : p.getCtaLabel())
                .ctaUrl(p.getCtaUrl())
                .sponsorNom(sponsorNom)
                .build();
    }

    private GameDTO toGameDTO(Jeu jeu) {
        return GameDTO.builder()
                .id(jeu.getId())
                .titre(jeu.getTitre())
                .description(jeu.getDescription())
                .difficulte(jeu.getDifficulte())
                .ageMin(jeu.getAgeMin())
                .ageMax(jeu.getAgeMax())
                .typeJeu(jeu.getTypeJeu())
                .modeJeu(jeu.getModeJeu())
                .quizVariant(jeu.getQuizVariant())
                .actif(jeu.isActif())
                .dureeMinutes(jeu.getDureeMinutes())
                .coverImageUrl(jeu.getCoverImageUrl())
                .etat(jeu.getEtat())
                .dateCreation(jeu.getDateCreation())
                .build();
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw ApiException.unauthorized("Utilisateur non authentifié");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
    }

    private User ensureSponsorAccess(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        if (user.getRole() != Role.SPONSOR && user.getRole() != Role.ADMIN) {
            throw ApiException.unauthorized("Accès sponsor requis");
        }
        return user;
    }

    private User ensurePlayerAccess(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        if (user.getRole() != Role.JOUEUR && user.getRole() != Role.ADMIN) {
            throw ApiException.unauthorized("Accès joueur requis");
        }
        return user;
    }

    private boolean isAdmin(User user) {
        return user != null && user.getRole() == Role.ADMIN;
    }

    private RecompenseDTO toRecompenseDTOFromExternal(com.fasterxml.jackson.databind.JsonNode n) {
        return RecompenseDTO.builder()
                .id(n.path("id").asLong(0))
                .nom(firstText(n, "name", "nom"))
                .description(firstText(n, "description"))
                .scoreMin(firstInt(n, "scoreMin", "pointsCost"))
                .typeRecompense(firstText(n, "type", "typeRecompense"))
                .sponsorNom(firstText(n, "sponsorName", "sponsorNom"))
                .stockTotal(firstInt(n, "stockTotal", "stock", "quantity"))
                .stockRemaining(firstInt(n, "stockRemaining", "stockLeft", "remaining"))
                .distributedCount(firstInt(n, "distributedCount", "distributed"))
                .valeur(firstDouble(n, "value", "valeur", "amount"))
                .devise(firstText(n, "currency", "devise"))
                .partenaireNom(firstText(n, "partnerName", "partenaireNom"))
                .dateEvenement(firstText(n, "eventDate", "dateEvenement", "expiresAt"))
                .lieuEvenement(firstText(n, "eventLocation", "lieuEvenement"))
                .modeRemise(firstText(n, "redemptionMode", "modeRemise"))
                .instructionsRemise(firstText(n, "redemptionInstructions", "instructionsRemise"))
                .imageUrl(firstText(n, "imageUrl", "thumbnailUrl"))
                .status(firstText(n, "status"))
                .build();
    }

    private RecompenseDTO toRecompenseDTOLocal(Recompense r) {
        User sponsor = r.getSponsor();
        String sponsorName = null;
        if (sponsor != null) {
            sponsorName = ((sponsor.getPrenom() != null ? sponsor.getPrenom() : "")
                    + " " + (sponsor.getNom() != null ? sponsor.getNom() : "")).trim();
            if (sponsorName.isBlank()) {
                sponsorName = sponsor.getEmail();
            }
        }
        return RecompenseDTO.builder()
                .id(r.getId())
                .nom(r.getNom())
                .description(r.getDescription())
                .scoreMin(r.getScoreMin())
                .typeRecompense(r.getTypeRecompense() == null ? null : r.getTypeRecompense().name())
                .sponsorId(sponsor != null ? sponsor.getId() : null)
                .sponsorNom(sponsorName)
                .sponsorEmail(sponsor != null ? sponsor.getEmail() : null)
                .status(Boolean.FALSE.equals(r.getActive()) ? "INACTIVE" : "ACTIVE")
                .build();
    }

    private RecompenseDTO toRecompenseDTOFromExternalFallbackActive(com.fasterxml.jackson.databind.JsonNode n) {
        RecompenseDTO dto = toRecompenseDTOFromExternal(n);
        if (dto.getStatus() == null || dto.getStatus().isBlank()) {
            dto.setStatus("ACTIVE");
        }
        return dto;
    }

    private TypeRecompense parseTypeRecompense(String rawType) {
        try {
            return TypeRecompense.valueOf(rawType.trim().toUpperCase());
        } catch (Exception ex) {
            throw ApiException.badRequest("Type de récompense invalide. Valeurs: BON_D_ACHAT, REDUCTION, CADEAU, AUTRE");
        }
    }

    private TypeRecompense parseTypeRecompenseOrDefault(String rawType) {
        if (rawType == null || rawType.isBlank()) return TypeRecompense.AUTRE;
        try {
            return TypeRecompense.valueOf(rawType.trim().toUpperCase());
        } catch (Exception ex) {
            return TypeRecompense.AUTRE;
        }
    }

    private String normalizeRewardRequestStatus(String status) {
        if (status == null || status.isBlank()) {
            throw ApiException.badRequest("Le statut est requis");
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "PENDING", "APPROVED", "REJECTED" -> normalized;
            default -> throw ApiException.badRequest("Statut invalide. Valeurs : PENDING, APPROVED, REJECTED");
        };
    }

    private SponsorRewardRequestDTO toSponsorRewardRequestDTO(DemandeRecompense request) {
        User player = request.getUtilisateur();
        Recompense reward = request.getRecompense();
        String playerName = player != null ? ((player.getPrenom() != null ? player.getPrenom() : "") + " " + (player.getNom() != null ? player.getNom() : "")).trim() : null;
        return SponsorRewardRequestDTO.builder()
                .id(request.getId())
                .rewardId(reward != null ? reward.getId() : null)
                .rewardName(reward != null ? reward.getNom() : null)
                .rewardScoreMin(reward != null ? reward.getScoreMin() : null)
                .playerId(player != null ? player.getId() : null)
                .playerName(playerName == null || playerName.isBlank() ? null : playerName)
                .playerEmail(player != null ? player.getEmail() : null)
                .playerScoreTotal(player != null ? player.getScoreTotal() : null)
                .status(request.getStatut())
                .requestedDate(request.getDateDemande())
                .build();
    }

    private Map<String, Object> toRewardPayload(CreateRecompenseRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", request.getNom());
        payload.put("description", request.getDescription());
        payload.put("pointsCost", request.getScoreMin());
        payload.put("type", request.getTypeRecompense());
        return payload;
    }

    private Map<String, Object> toRewardPayload(UpdateRecompenseRequest request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("name", request.getNom());
        payload.put("description", request.getDescription());
        payload.put("pointsCost", request.getScoreMin());
        payload.put("type", request.getTypeRecompense());
        return payload;
    }

    private String firstText(com.fasterxml.jackson.databind.JsonNode node, String... fields) {
        for (String f : fields) {
            com.fasterxml.jackson.databind.JsonNode v = node.path(f);
            if (!v.isMissingNode() && !v.isNull() && !v.asText("").isBlank()) {
                return v.asText();
            }
        }
        return null;
    }

    private Integer firstInt(com.fasterxml.jackson.databind.JsonNode node, String... fields) {
        for (String f : fields) {
            com.fasterxml.jackson.databind.JsonNode v = node.path(f);
            if (!v.isMissingNode() && !v.isNull()) {
                return v.asInt(0);
            }
        }
        return 0;
    }

    private Double firstDouble(com.fasterxml.jackson.databind.JsonNode node, String... fields) {
        for (String f : fields) {
            com.fasterxml.jackson.databind.JsonNode v = node.path(f);
            if (!v.isMissingNode() && !v.isNull()) {
                return v.asDouble(0.0);
            }
        }
        return 0.0;
    }

    private String requireText(String value, String message) {
        if (value == null || value.isBlank()) {
            throw ApiException.badRequest(message);
        }
        return value.trim();
    }

    private String trimToNull(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String firstNonBlank(String... values) {
        if (values == null) return null;
        for (String value : values) {
            if (value != null && !value.isBlank()) {
                return value.trim();
            }
        }
        return null;
    }
}
