package com.stage.auth.authbackend.service.admin;

import com.stage.auth.authbackend.dto.game.CreateGameRequest;
import com.stage.auth.authbackend.dto.game.GameDTO;
import com.stage.auth.authbackend.dto.game.UpdateGameRequest;
import com.stage.auth.authbackend.entity.EtatJeu;
import com.stage.auth.authbackend.entity.GameReviewAction;
import com.stage.auth.authbackend.entity.GameReviewHistory;
import com.stage.auth.authbackend.entity.Jeu;
import com.stage.auth.authbackend.entity.User;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.game.GameReviewHistoryRepository;
import com.stage.auth.authbackend.repository.game.JeuRepository;
import com.stage.auth.authbackend.repository.user.UserRepository;
import com.stage.auth.authbackend.service.auth.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminGameService {

    private final JeuRepository jeuRepository;
    private final GameReviewHistoryRepository gameReviewHistoryRepository;
    private final UserRepository userRepository;
    private final EmailService emailService;

    /**
     * Liste tous les jeux (réservé à l'admin).
     */
    public List<GameDTO> findAllGames() {
        return jeuRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    /**
     * Récupère un jeu par id (réservé à l'admin).
     */
    public GameDTO findGameById(Long id) {
        Jeu jeu = jeuRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        return toDTO(jeu);
    }

    /**
     * Met à jour un jeu (réservé à l'admin). Seuls les champs non null du request sont appliqués.
     */
    public GameDTO updateGame(Long id, UpdateGameRequest request) {
        Jeu jeu = jeuRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        if (request.getTitre() != null) jeu.setTitre(request.getTitre().trim());
        if (request.getDescription() != null) jeu.setDescription(request.getDescription().trim());
        if (request.getDifficulte() != null) jeu.setDifficulte(request.getDifficulte());
        if (request.getAgeMin() != null) jeu.setAgeMin(request.getAgeMin());
        if (request.getAgeMax() != null) jeu.setAgeMax(request.getAgeMax());
        if (request.getTypeJeu() != null) jeu.setTypeJeu(request.getTypeJeu());
        if (request.getModeJeu() != null) jeu.setModeJeu(request.getModeJeu());
        if (request.getDureeMinutes() != null) jeu.setDureeMinutes(request.getDureeMinutes());
        if (request.getIcone() != null) jeu.setIcone(request.getIcone().trim().isEmpty() ? null : request.getIcone().trim());
        if (request.getCoverImageUrl() != null) jeu.setCoverImageUrl(request.getCoverImageUrl().trim().isEmpty() ? null : request.getCoverImageUrl().trim());
        if (request.getActif() != null) jeu.setActif(request.getActif());
        jeu = jeuRepository.save(jeu);
        return toDTO(jeu);
    }

    /**
     * Supprime un jeu (réservé à l'admin).
     */
    public void deleteGame(Long id) {
        Jeu jeu = jeuRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        jeuRepository.delete(jeu);
    }

    /**
     * Accepter ou refuser un jeu
     */
    public GameDTO changeGameState(Long id, EtatJeu etat, String motifRefus, String adminEmail) {
        if (etat == EtatJeu.BROUILLON) {
            throw ApiException.badRequest("L'état BROUILLON est réservé à l'éducateur");
        }
        if (etat == EtatJeu.EN_ATTENTE) {
            throw ApiException.badRequest("L'état EN_ATTENTE est réservé à la soumission éducateur");
        }
        Jeu jeu = jeuRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));

        if (jeu.getEtat() != EtatJeu.EN_ATTENTE) {
            throw ApiException.badRequest("Seuls les jeux en attente peuvent être traités");
        }

        String normalizedMotif = motifRefus != null ? motifRefus.trim() : null;
        if (etat == EtatJeu.REFUSE && (normalizedMotif == null || normalizedMotif.isBlank())) {
            throw ApiException.badRequest("Le motif de refus est obligatoire");
        }

        User admin = null;
        if (adminEmail != null && !adminEmail.isBlank()) {
            admin = userRepository.findByEmail(adminEmail.trim().toLowerCase()).orElse(null);
        }

        jeu.setEtat(etat);
        jeu = jeuRepository.save(jeu);

        GameReviewHistory review = GameReviewHistory.builder()
                .jeu(jeu)
                .admin(admin)
                .action(etat == EtatJeu.ACCEPTE ? GameReviewAction.ACCEPTE : GameReviewAction.REFUSE)
                .motifRefus(etat == EtatJeu.REFUSE ? normalizedMotif : null)
                .createdAt(LocalDateTime.now())
                .build();
        gameReviewHistoryRepository.save(review);

        if (etat == EtatJeu.ACCEPTE) {
            if (jeu.getEducateur() != null
                    && jeu.getEducateur().getEmail() != null
                    && !jeu.getEducateur().getEmail().isBlank()) {
                emailService.sendGameApprovedEmail(jeu.getEducateur().getEmail(), jeu.getTitre());
            }
        } else if (etat == EtatJeu.REFUSE) {
            if (jeu.getEducateur() != null
                    && jeu.getEducateur().getEmail() != null
                    && !jeu.getEducateur().getEmail().isBlank()) {
                emailService.sendGameRejectedEmail(jeu.getEducateur().getEmail(), jeu.getTitre(), normalizedMotif);
            }
        }

        return toDTO(jeu);
    }

    /**
     * Crée un nouveau jeu (réservé à l'admin).
     */
    public GameDTO createGame(CreateGameRequest request) {
        Jeu jeu = Jeu.builder()
                .titre(request.getTitre().trim())
                .description(request.getDescription() != null ? request.getDescription().trim() : null)
                .difficulte(request.getDifficulte())
                .ageMin(request.getAgeMin())
                .ageMax(request.getAgeMax())
                .typeJeu(request.getTypeJeu())
                .modeJeu(request.getModeJeu())
                .dureeMinutes(request.getDureeMinutes())
                .icone(request.getIcone() != null && !request.getIcone().trim().isEmpty() ? request.getIcone().trim() : null)
                .coverImageUrl(request.getCoverImageUrl() != null && !request.getCoverImageUrl().trim().isEmpty() ? request.getCoverImageUrl().trim() : null)
                .actif(request.getActif() != null ? request.getActif() : true)
                .etat(EtatJeu.ACCEPTE)
                .dateCreation(LocalDateTime.now())
                .build();
        jeu = jeuRepository.save(jeu);
        return toDTO(jeu);
    }

    private GameDTO toDTO(Jeu jeu) {
        String latestRefusalReason = gameReviewHistoryRepository.findTopByJeuIdOrderByCreatedAtDescIdDesc(jeu.getId())
                .filter(r -> r.getAction() == GameReviewAction.REFUSE)
                .map(GameReviewHistory::getMotifRefus)
                .orElse(null);

        return GameDTO.builder()
                .id(jeu.getId())
                .titre(jeu.getTitre())
                .description(jeu.getDescription())
                .difficulte(jeu.getDifficulte())
                .ageMin(jeu.getAgeMin())
                .ageMax(jeu.getAgeMax())
                .typeJeu(jeu.getTypeJeu())
                .modeJeu(jeu.getModeJeu())
                .actif(jeu.isActif())
                .dureeMinutes(jeu.getDureeMinutes())
                .icone(jeu.getIcone())
                .coverImageUrl(jeu.getCoverImageUrl())
                .etat(jeu.getEtat())
                .latestRefusalReason(latestRefusalReason)
                .dateCreation(jeu.getDateCreation())
                .build();
    }
}
