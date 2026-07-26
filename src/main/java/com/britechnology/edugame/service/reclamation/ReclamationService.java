package com.britechnology.edugame.service.reclamation;

import com.britechnology.edugame.dto.reclamation.CreateReclamationRequest;
import com.britechnology.edugame.dto.reclamation.ReclamationDTO;
import com.britechnology.edugame.dto.reclamation.UpdateReclamationRequest;
import com.britechnology.edugame.entity.EtatJeu;
import com.britechnology.edugame.entity.MotifReclamation;
import com.britechnology.edugame.entity.Reclamation;
import com.britechnology.edugame.entity.Role;
import com.britechnology.edugame.entity.StatutReclamation;
import com.britechnology.edugame.entity.SessionJeu;
import com.britechnology.edugame.entity.User;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.game.SessionJeuRepository;
import com.britechnology.edugame.repository.reclamation.ReclamationRepository;
import com.britechnology.edugame.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ReclamationService {

    private static final int COMMENTAIRE_MAX = 1000;
    private static final int AUTRE_MIN_LENGTH = 10;

    private final ReclamationRepository reclamationRepository;
    private final SessionJeuRepository sessionJeuRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReclamationDTO create(Authentication authentication, CreateReclamationRequest request) {
        User player = requireJoueur(authentication);
        validateCreateRequest(request);

        SessionJeu session = sessionJeuRepository.findById(request.getSessionId())
                .orElseThrow(() -> ApiException.notFound("Session introuvable"));

        if (!session.getUtilisateur().getId().equals(player.getId())) {
            throw ApiException.forbidden("Cette session ne vous appartient pas");
        }
        if (session.getJeu() == null || !session.getJeu().getId().equals(request.getGameId())) {
            throw ApiException.badRequest("Le jeu ne correspond pas à la session");
        }
        if (session.getJeu().getEtat() != EtatJeu.ACCEPTE) {
            throw ApiException.badRequest("Ce jeu n'est pas disponible pour un signalement");
        }

        if (reclamationRepository.existsBySessionJeuId(session.getId())) {
            throw ApiException.badRequest("Un signalement existe déjà pour cette session");
        }

        String commentaire = normalizeCommentaire(request.getCommentaire());
        if (request.getMotif() == MotifReclamation.AUTRE) {
            if (commentaire == null || commentaire.length() < AUTRE_MIN_LENGTH) {
                throw ApiException.badRequest("Le motif « Autre » nécessite un commentaire d'au moins 10 caractères");
            }
        }

        Reclamation saved = reclamationRepository.save(Reclamation.builder()
                .utilisateur(player)
                .jeu(session.getJeu())
                .sessionJeu(session)
                .motif(request.getMotif())
                .commentaire(commentaire)
                .statut(StatutReclamation.OUVERT)
                .createdAt(LocalDateTime.now())
                .build());

        return toDTO(saved);
    }

    @Transactional(readOnly = true)
    public List<ReclamationDTO> listForAdmin(Boolean pendingOnly) {
        List<Reclamation> rows = Boolean.TRUE.equals(pendingOnly)
                ? reclamationRepository.findByStatutOrderByCreatedAtDesc(StatutReclamation.OUVERT)
                : reclamationRepository.findAllByOrderByCreatedAtDesc();
        return rows.stream().map(this::toDTO).toList();
    }

    @Transactional(readOnly = true)
    public long countPending() {
        return reclamationRepository.countByStatut(StatutReclamation.OUVERT);
    }

    @Transactional
    public ReclamationDTO updateByAdmin(Authentication authentication, Long id, UpdateReclamationRequest request) {
        User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (admin.getRole() != Role.ADMIN) {
            throw ApiException.forbidden("Réservé à l'administration");
        }

        Reclamation reclamation = reclamationRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Réclamation introuvable"));

        if (reclamation.getStatut() != StatutReclamation.OUVERT) {
            throw ApiException.badRequest("Cette réclamation a déjà été traitée");
        }

        StatutReclamation newStatut = request != null && request.getStatut() != null
                ? request.getStatut()
                : StatutReclamation.TRAITE;
        if (newStatut == StatutReclamation.OUVERT) {
            throw ApiException.badRequest("Le statut OUVERT n'est pas autorisé lors du traitement admin");
        }

        String reponse = request != null && request.getReponseAdmin() != null
                ? request.getReponseAdmin().trim()
                : "";
        if (reponse.isBlank()) {
            reponse = newStatut == StatutReclamation.REJETE
                    ? "Réclamation rejetée par l'administration."
                    : "Réclamation traitée par l'administration.";
        }
        if (reponse.length() > COMMENTAIRE_MAX) {
            throw ApiException.badRequest("La réponse admin est trop longue (max 1000 caractères)");
        }

        reclamation.setStatut(newStatut);
        reclamation.setReponseAdmin(reponse);
        reclamation.setAdmin(admin);
        reclamation.setUpdatedAt(LocalDateTime.now());

        return toDTO(reclamationRepository.save(reclamation));
    }

    private User requireJoueur(Authentication authentication) {
        User user = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));
        if (user.getRole() != Role.JOUEUR) {
            throw ApiException.badRequest("Seuls les joueurs peuvent envoyer un signalement");
        }
        return user;
    }

    private void validateCreateRequest(CreateReclamationRequest request) {
        if (request.getSessionId() == null || request.getGameId() == null || request.getMotif() == null) {
            throw ApiException.badRequest("sessionId, gameId et motif sont obligatoires");
        }
    }

    private String normalizeCommentaire(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.isEmpty()) return null;
        if (trimmed.length() > COMMENTAIRE_MAX) {
            throw ApiException.badRequest("Le commentaire est trop long (max 1000 caractères)");
        }
        return trimmed;
    }

    private ReclamationDTO toDTO(Reclamation r) {
        User player = r.getUtilisateur();
        User admin = r.getAdmin();
        return ReclamationDTO.builder()
                .id(r.getId())
                .gameId(r.getJeu() != null ? r.getJeu().getId() : null)
                .gameTitle(r.getJeu() != null ? r.getJeu().getTitre() : null)
                .gameType(r.getJeu() != null ? r.getJeu().getTypeJeu() : null)
                .sessionId(r.getSessionJeu() != null ? r.getSessionJeu().getId() : null)
                .playerId(player != null ? player.getId() : null)
                .playerPrenom(player != null ? player.getPrenom() : null)
                .playerNom(player != null ? player.getNom() : null)
                .playerEmail(player != null ? player.getEmail() : null)
                .motif(r.getMotif())
                .commentaire(r.getCommentaire())
                .statut(r.getStatut())
                .reponseAdmin(r.getReponseAdmin())
                .adminId(admin != null ? admin.getId() : null)
                .adminPrenom(admin != null ? admin.getPrenom() : null)
                .adminNom(admin != null ? admin.getNom() : null)
                .traitee(r.isTraitee())
                .createdAt(r.getCreatedAt())
                .updatedAt(r.getUpdatedAt())
                .build();
    }
}
