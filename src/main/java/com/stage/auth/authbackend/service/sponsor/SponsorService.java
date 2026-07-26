package com.stage.auth.authbackend.service.sponsor;

import com.fasterxml.jackson.databind.JsonNode;
import com.stage.auth.authbackend.dto.sponsor.*;
import com.stage.auth.authbackend.entity.*;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.reward.DemandeRecompenseRepository;
import com.stage.auth.authbackend.repository.sponsor.RecompenseRepository;
import com.stage.auth.authbackend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.StreamSupport;

@Service
@RequiredArgsConstructor
public class SponsorService {

    private final ExternalAdsClient externalAdsClient;
    private final UserRepository userRepository;
    private final RecompenseRepository recompenseRepository;
    private final DemandeRecompenseRepository demandeRecompenseRepository;

    public SponsorDashboardStatsDTO getDashboardStats(Authentication authentication) {
        ensureSponsorAccess(authentication);
        JsonNode payload = externalAdsClient.get("/stats");
        if (payload == null || payload.isNull()) {
            return SponsorDashboardStatsDTO.builder()
                    .activeCampaigns(0)
                    .totalImpressions(0)
                    .totalClicks(0)
                    .ctr(0.0)
                    .distributedRewards(0)
                    .rewardStock(0)
                    .build();
        }

        return SponsorDashboardStatsDTO.builder()
                .activeCampaigns(payload.path("activeCampaigns").asInt(0))
                .totalImpressions(payload.path("totalImpressions").asInt(0))
                .totalClicks(payload.path("totalClicks").asInt(0))
                .ctr(payload.path("ctr").asDouble(0.0))
                .distributedRewards(payload.path("distributedRewards").asInt(0))
                .rewardStock(payload.path("rewardStock").asInt(0))
                .build();
    }

    public List<PubliciteDTO> listPublicites(Authentication authentication) {
        ensureSponsorAccess(authentication);
        if (!externalAdsClient.isEnabled()) return List.of();
        JsonNode payload = externalAdsClient.get("/ads");
        if (payload == null || !payload.isArray()) return List.of();
        return StreamSupport.stream(payload.spliterator(), false)
                .map(this::toPubliciteDTOFromExternal)
                .toList();
    }

    public PubliciteDTO createPublicite(Authentication authentication, Map<String, Object> request) {
        ensureSponsorAccess(authentication);
        if (!externalAdsClient.isEnabled()) {
            throw ApiException.badRequest("Le provider externe est désactivé pour les publicités");
        }
        JsonNode created = externalAdsClient.post("/ads", request == null ? Map.of() : request);
        if (created == null || created.isNull()) {
            throw ApiException.badRequest("Le provider externe n'a pas pu créer la publicité");
        }
        return toPubliciteDTOFromExternal(created);
    }

    public PubliciteDTO updatePublicite(Authentication authentication, Long id, Map<String, Object> request) {
        ensureSponsorAccess(authentication);
        if (id == null) throw ApiException.badRequest("id est requis");
        if (!externalAdsClient.isEnabled()) {
            throw ApiException.badRequest("Le provider externe est désactivé pour les publicités");
        }
        JsonNode updated = externalAdsClient.put("/ads/" + id, request == null ? Map.of() : request);
        if (updated == null || updated.isNull()) {
            throw ApiException.badRequest("Le provider externe n'a pas pu modifier la publicité");
        }
        return toPubliciteDTOFromExternal(updated);
    }

    public PubliciteDTO setPubliciteStatus(Authentication authentication, Long id, boolean active) {
        ensureSponsorAccess(authentication);
        if (id == null) throw ApiException.badRequest("id est requis");
        if (!externalAdsClient.isEnabled()) {
            throw ApiException.badRequest("Le provider externe est désactivé pour les publicités");
        }
        JsonNode updated = externalAdsClient.patch("/ads/" + id + "/status", Map.of("active", active));
        if (updated == null || updated.isNull()) {
            throw ApiException.badRequest("Le provider externe n'a pas pu changer le statut de la publicité");
        }
        return toPubliciteDTOFromExternal(updated);
    }

    public void deletePublicite(Authentication authentication, Long id) {
        ensureSponsorAccess(authentication);
        if (id == null) throw ApiException.badRequest("id est requis");
        if (!externalAdsClient.isEnabled()) {
            throw ApiException.badRequest("Le provider externe est désactivé pour les publicités");
        }
        boolean deleted = externalAdsClient.delete("/ads/" + id);
        if (!deleted) {
            throw ApiException.badRequest("Le provider externe n'a pas pu supprimer la publicité");
        }
    }

    public List<RecompenseDTO> listRecompenses(Authentication authentication) {
        ensureSponsorAccess(authentication);
        List<Recompense> localRewards = recompenseRepository.findAllByOrderByIdDesc();
        if (!localRewards.isEmpty()) {
            return localRewards.stream().map(this::toRecompenseDTOLocal).toList();
        }
        if (!externalAdsClient.isEnabled()) return List.of();
        JsonNode payload = externalAdsClient.get("/rewards");
        if (payload == null || !payload.isArray()) return List.of();
        List<RecompenseDTO> externalRewards = StreamSupport.stream(payload.spliterator(), false)
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

    public RecompenseDTO getRecompenseById(Authentication authentication, Long id) {
        ensureSponsorAccess(authentication);
        if (id == null) throw ApiException.badRequest("id est requis");
        return recompenseRepository.findById(id)
                .map(this::toRecompenseDTOLocal)
                .orElseThrow(() -> ApiException.notFound("Récompense introuvable"));
    }

    public List<SponsorRewardRequestDTO> listRewardRequests(Authentication authentication) {
        ensureSponsorAccess(authentication);
        return demandeRecompenseRepository.findAllByOrderByDateDemandeDescIdDesc().stream()
                .map(this::toSponsorRewardRequestDTO)
                .toList();
    }

    public SponsorRewardRequestDTO updateRewardRequestStatus(Authentication authentication, Long requestId, String status) {
        ensureSponsorAccess(authentication);
        if (requestId == null) throw ApiException.badRequest("requestId est requis");
        String normalized = normalizeRewardRequestStatus(status);
        DemandeRecompense request = demandeRecompenseRepository.findById(requestId)
                .orElseThrow(() -> ApiException.notFound("Demande de récompense introuvable"));
        request.setStatut(normalized);
        return toSponsorRewardRequestDTO(demandeRecompenseRepository.save(request));
    }

    public RecompenseDTO createRecompense(Authentication authentication, CreateRecompenseRequest request) {
        ensureSponsorAccess(authentication);
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
                .build();
        Recompense saved = recompenseRepository.save(reward);
        if (externalAdsClient.isEnabled()) {
            externalAdsClient.post("/rewards", toRewardPayload(request));
        }
        return toRecompenseDTOLocal(saved);
    }

    public RecompenseDTO updateRecompense(Authentication authentication, Long id, UpdateRecompenseRequest request) {
        ensureSponsorAccess(authentication);
        if (id == null) throw ApiException.badRequest("id est requis");
        Recompense reward = recompenseRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Récompense introuvable"));
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

    public RecompenseDTO setRecompenseStatus(Authentication authentication, Long id, boolean active) {
        ensureSponsorAccess(authentication);
        if (id == null) throw ApiException.badRequest("id est requis");
        Recompense reward = recompenseRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Récompense introuvable"));
        reward.setActive(active);
        Recompense saved = recompenseRepository.save(reward);
        if (externalAdsClient.isEnabled()) {
            externalAdsClient.patch("/rewards/" + id + "/status", Map.of("active", active));
        }
        return toRecompenseDTOLocal(saved);
    }

    public void deleteRecompense(Authentication authentication, Long id) {
        ensureSponsorAccess(authentication);
        if (id == null) throw ApiException.badRequest("id est requis");
        if (!recompenseRepository.existsById(id)) {
            throw ApiException.notFound("Récompense introuvable");
        }
        recompenseRepository.deleteById(id);
        if (externalAdsClient.isEnabled()) {
            externalAdsClient.delete("/rewards/" + id);
        }
    }

    private User resolveAuthenticatedUser(Authentication authentication) {
        if (authentication == null || authentication.getName() == null) {
            throw ApiException.unauthorized("Utilisateur non authentifié");
        }
        return userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
    }

    private void ensureSponsorAccess(Authentication authentication) {
        User user = resolveAuthenticatedUser(authentication);
        if (user.getRole() != Role.SPONSOR && user.getRole() != Role.ADMIN) {
            throw ApiException.unauthorized("Accès sponsor requis");
        }
    }

    private RecompenseDTO toRecompenseDTOFromExternal(JsonNode n) {
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

    private PubliciteDTO toPubliciteDTOFromExternal(JsonNode n) {
        return PubliciteDTO.builder()
                .id(n.path("id").asLong(0))
                .contenu(firstText(n, "contenu", "content", "title"))
                .status(firstText(n, "status"))
                .typePublicite(firstText(n, "typePublicite", "type"))
                .imageUrl(firstText(n, "imageUrl", "mediaUrl"))
                .adDurationSeconds(firstInt(n, "adDurationSeconds", "durationSeconds"))
                .ctaLabel(firstText(n, "ctaLabel"))
                .ctaUrl(firstText(n, "ctaUrl"))
                .budgetUtilise(firstDouble(n, "budgetUtilise", "budgetSpent"))
                .nbVues(firstInt(n, "nbVues", "views", "impressions"))
                .nbClics(firstInt(n, "nbClics", "clicks"))
                .sponsorNom(firstText(n, "sponsorNom", "sponsorName"))
                .build();
    }

    private RecompenseDTO toRecompenseDTOLocal(Recompense r) {
        return RecompenseDTO.builder()
                .id(r.getId())
                .nom(r.getNom())
                .description(r.getDescription())
                .scoreMin(r.getScoreMin())
                .typeRecompense(r.getTypeRecompense() == null ? null : r.getTypeRecompense().name())
                .status(Boolean.FALSE.equals(r.getActive()) ? "INACTIVE" : "ACTIVE")
                .build();
    }

    private RecompenseDTO toRecompenseDTOFromExternalFallbackActive(JsonNode n) {
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
            throw ApiException.badRequest("status est requis");
        }
        String normalized = status.trim().toUpperCase();
        return switch (normalized) {
            case "PENDING", "APPROVED", "REJECTED" -> normalized;
            default -> throw ApiException.badRequest("Status invalide. Valeurs: PENDING, APPROVED, REJECTED");
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

    private String firstText(JsonNode node, String... fields) {
        for (String f : fields) {
            JsonNode v = node.path(f);
            if (!v.isMissingNode() && !v.isNull() && !v.asText("").isBlank()) {
                return v.asText();
            }
        }
        return null;
    }

    private Integer firstInt(JsonNode node, String... fields) {
        for (String f : fields) {
            JsonNode v = node.path(f);
            if (!v.isMissingNode() && !v.isNull()) {
                return v.asInt(0);
            }
        }
        return 0;
    }

    private Double firstDouble(JsonNode node, String... fields) {
        for (String f : fields) {
            JsonNode v = node.path(f);
            if (!v.isMissingNode() && !v.isNull()) {
                return v.asDouble(0.0);
            }
        }
        return 0.0;
    }

}
