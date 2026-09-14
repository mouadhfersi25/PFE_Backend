package com.britechnology.edugame.service.admin;

import com.britechnology.edugame.dto.game.CreateGameRequest;
import com.britechnology.edugame.dto.game.GameDTO;
import com.britechnology.edugame.dto.game.UpdateGameRequest;
import com.britechnology.edugame.entity.EtatJeu;
import com.britechnology.edugame.entity.GameReviewAction;
import com.britechnology.edugame.entity.GameReviewHistory;
import com.britechnology.edugame.entity.Jeu;
import com.britechnology.edugame.entity.QuizVariant;
import com.britechnology.edugame.entity.TypeJeu;
import com.britechnology.edugame.entity.User;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.game.GameReviewHistoryRepository;
import com.britechnology.edugame.repository.game.JeuRepository;
import com.britechnology.edugame.repository.game.SessionJeuRepository;
import com.britechnology.edugame.repository.user.UserRepository;
import com.britechnology.edugame.service.auth.EmailService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminGameService {

    private final JeuRepository jeuRepository;
    private final GameReviewHistoryRepository gameReviewHistoryRepository;
    private final UserRepository userRepository;
    private final SessionJeuRepository sessionJeuRepository;
    private final EmailService emailService;

    /**
     * Liste tous les jeux (réservé à l'admin).
     */
    @Transactional(readOnly = true)
    public List<GameDTO> findAllGames() {
        Map<Long, Long> sessionCounts = new HashMap<>();
        for (Object[] row : sessionJeuRepository.countSessionsGroupedByJeu()) {
            sessionCounts.put((Long) row[0], (Long) row[1]);
        }
        return jeuRepository.findAll().stream()
                .map(jeu -> toDTO(jeu, sessionCounts))
                .collect(Collectors.toList());
    }

    /**
     * Récupère un jeu par id (réservé à l'admin).
     */
    @Transactional(readOnly = true)
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
        if (request.getCoverImageUrl() != null) jeu.setCoverImageUrl(request.getCoverImageUrl().trim().isEmpty() ? null : request.getCoverImageUrl().trim());
        if (request.getActif() != null) jeu.setActif(request.getActif());
        if (request.getQuizVariant() != null) {
            TypeJeu effectiveType = request.getTypeJeu() != null ? request.getTypeJeu() : jeu.getTypeJeu();
            jeu.setQuizVariant(resolveQuizVariant(effectiveType, request.getQuizVariant()));
        }
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
    @Transactional
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
     * Désactive un jeu déjà accepté, suite à un signalement joueur validé par l'admin.
     * Contrairement à {@link #changeGameState}, ne touche pas à l'état de modération (etat
     * reste ACCEPTE) : seul le flag "actif" passe à false, le jeu disparaît donc de l'offre
     * des joueurs sans repasser par le circuit de validation initial. Notifie l'éducateur
     * propriétaire par email avec le détail fourni par l'admin.
     */
    @Transactional
    public GameDTO deactivateGame(Long id, String motif, String adminEmail) {
        Jeu jeu = jeuRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));

        if (!jeu.isActif()) {
            throw ApiException.badRequest("Ce jeu est déjà désactivé");
        }

        String normalizedMotif = motif != null ? motif.trim() : null;
        if (normalizedMotif == null || normalizedMotif.isBlank()) {
            throw ApiException.badRequest("Les détails de désactivation sont obligatoires");
        }

        User admin = resolveAdmin(adminEmail);

        jeu.setActif(false);
        jeu.setReactivationPending(false);
        jeu = jeuRepository.save(jeu);

        gameReviewHistoryRepository.save(GameReviewHistory.builder()
                .jeu(jeu)
                .admin(admin)
                .action(GameReviewAction.DESACTIVE)
                .motifRefus(normalizedMotif)
                .createdAt(LocalDateTime.now())
                .build());

        if (jeu.getEducateur() != null
                && jeu.getEducateur().getEmail() != null
                && !jeu.getEducateur().getEmail().isBlank()) {
            emailService.sendGameDeactivatedEmail(jeu.getEducateur().getEmail(), jeu.getTitre(), normalizedMotif);
        }

        return toDTO(jeu);
    }

    /**
     * Réactive directement un jeu désactivé, à l'initiative de l'admin (sans passer par une
     * demande de réactivation de l'éducateur). Notifie l'éducateur que son jeu est de nouveau actif.
     */
    @Transactional
    public GameDTO activateGame(Long id, String adminEmail) {
        Jeu jeu = jeuRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));

        if (jeu.isActif()) {
            throw ApiException.badRequest("Ce jeu est déjà actif");
        }

        User admin = resolveAdmin(adminEmail);

        jeu.setActif(true);
        jeu.setReactivationPending(false);
        jeu = jeuRepository.save(jeu);

        gameReviewHistoryRepository.save(GameReviewHistory.builder()
                .jeu(jeu)
                .admin(admin)
                .action(GameReviewAction.REACTIVATION_ACCEPTEE)
                .createdAt(LocalDateTime.now())
                .build());

        if (jeu.getEducateur() != null
                && jeu.getEducateur().getEmail() != null
                && !jeu.getEducateur().getEmail().isBlank()) {
            emailService.sendReactivationAcceptedEmail(jeu.getEducateur().getEmail(), jeu.getTitre());
        }

        return toDTO(jeu);
    }

    /**
     * Accepte la demande de réactivation d'un jeu corrigé par l'éducateur après désactivation :
     * le jeu redevient actif et visible des joueurs. Notifie l'éducateur.
     */
    @Transactional
    public GameDTO acceptReactivation(Long id, String adminEmail) {
        Jeu jeu = jeuRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));

        if (!jeu.isReactivationPending()) {
            throw ApiException.badRequest("Aucune demande de réactivation en attente pour ce jeu");
        }

        User admin = resolveAdmin(adminEmail);

        jeu.setActif(true);
        jeu.setReactivationPending(false);
        jeu = jeuRepository.save(jeu);

        gameReviewHistoryRepository.save(GameReviewHistory.builder()
                .jeu(jeu)
                .admin(admin)
                .action(GameReviewAction.REACTIVATION_ACCEPTEE)
                .createdAt(LocalDateTime.now())
                .build());

        if (jeu.getEducateur() != null
                && jeu.getEducateur().getEmail() != null
                && !jeu.getEducateur().getEmail().isBlank()) {
            emailService.sendReactivationAcceptedEmail(jeu.getEducateur().getEmail(), jeu.getTitre());
        }

        return toDTO(jeu);
    }

    /**
     * Refuse la demande de réactivation : le jeu reste désactivé, l'éducateur est notifié du motif
     * et peut à nouveau corriger le jeu puis redemander une réactivation.
     */
    @Transactional
    public GameDTO rejectReactivation(Long id, String motif, String adminEmail) {
        Jeu jeu = jeuRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));

        if (!jeu.isReactivationPending()) {
            throw ApiException.badRequest("Aucune demande de réactivation en attente pour ce jeu");
        }

        String normalizedMotif = motif != null ? motif.trim() : null;
        if (normalizedMotif == null || normalizedMotif.isBlank()) {
            throw ApiException.badRequest("Le motif de refus est obligatoire");
        }

        User admin = resolveAdmin(adminEmail);

        jeu.setReactivationPending(false);
        jeu = jeuRepository.save(jeu);

        gameReviewHistoryRepository.save(GameReviewHistory.builder()
                .jeu(jeu)
                .admin(admin)
                .action(GameReviewAction.REACTIVATION_REFUSEE)
                .motifRefus(normalizedMotif)
                .createdAt(LocalDateTime.now())
                .build());

        if (jeu.getEducateur() != null
                && jeu.getEducateur().getEmail() != null
                && !jeu.getEducateur().getEmail().isBlank()) {
            emailService.sendReactivationRejectedEmail(jeu.getEducateur().getEmail(), jeu.getTitre(), normalizedMotif);
        }

        return toDTO(jeu);
    }

    private User resolveAdmin(String adminEmail) {
        if (adminEmail == null || adminEmail.isBlank()) return null;
        return userRepository.findByEmail(adminEmail.trim().toLowerCase()).orElse(null);
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
                .coverImageUrl(request.getCoverImageUrl() != null && !request.getCoverImageUrl().trim().isEmpty() ? request.getCoverImageUrl().trim() : null)
                .actif(request.getActif() != null ? request.getActif() : true)
                .quizVariant(resolveQuizVariant(request.getTypeJeu(), request.getQuizVariant()))
                .etat(EtatJeu.ACCEPTE)
                .dateCreation(LocalDateTime.now())
                .build();
        jeu = jeuRepository.save(jeu);
        return toDTO(jeu);
    }

    private GameDTO toDTO(Jeu jeu) {
        return toDTO(jeu, null);
    }

    /**
     * @param sessionCounts si fourni (cas de la liste complète), évite une requête de comptage par
     *                      jeu ; sinon (cas d'un seul jeu), le nombre de parties est requêté directement.
     */
    private GameDTO toDTO(Jeu jeu, Map<Long, Long> sessionCounts) {
        long sessionsCount = sessionCounts != null
                ? sessionCounts.getOrDefault(jeu.getId(), 0L)
                : sessionJeuRepository.countByJeuId(jeu.getId());

        String latestRefusalReason = gameReviewHistoryRepository.findTopByJeuIdOrderByCreatedAtDescIdDesc(jeu.getId())
                .filter(r -> r.getAction() == GameReviewAction.REFUSE)
                .map(GameReviewHistory::getMotifRefus)
                .orElse(null);
        String latestDeactivationReason = gameReviewHistoryRepository
                .findTopByJeuIdAndActionOrderByCreatedAtDescIdDesc(jeu.getId(), GameReviewAction.DESACTIVE)
                .map(GameReviewHistory::getMotifRefus)
                .orElse(null);
        String latestReactivationRejectionReason = gameReviewHistoryRepository
                .findTopByJeuIdAndActionOrderByCreatedAtDescIdDesc(jeu.getId(), GameReviewAction.REACTIVATION_REFUSEE)
                .map(GameReviewHistory::getMotifRefus)
                .orElse(null);

        User educateur = jeu.getEducateur();

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
                .reactivationPending(jeu.isReactivationPending())
                .dureeMinutes(jeu.getDureeMinutes())
                .coverImageUrl(jeu.getCoverImageUrl())
                .educatorId(educateur != null ? educateur.getId() : null)
                .educatorName(educateur != null ? buildFullName(educateur) : null)
                .educatorEmail(educateur != null ? educateur.getEmail() : null)
                .sessionsCount(sessionsCount)
                .etat(jeu.getEtat())
                .latestRefusalReason(latestRefusalReason)
                .latestDeactivationReason(latestDeactivationReason)
                .latestReactivationRejectionReason(latestReactivationRejectionReason)
                .dateCreation(jeu.getDateCreation())
                .build();
    }

    private String buildFullName(User user) {
        String prenom = user.getPrenom() != null ? user.getPrenom().trim() : "";
        String nom = user.getNom() != null ? user.getNom().trim() : "";
        String fullName = (prenom + " " + nom).trim();
        if (!fullName.isEmpty()) return fullName;
        return user.getEmail();
    }

    private QuizVariant resolveQuizVariant(TypeJeu typeJeu, QuizVariant requested) {
        if (typeJeu != TypeJeu.QUIZ) {
            return QuizVariant.DEFAULT;
        }
        return requested != null ? requested : QuizVariant.DEFAULT;
    }
}
