package com.stage.auth.authbackend.service.educator;

import com.stage.auth.authbackend.dto.game.GameDTO;
import com.stage.auth.authbackend.dto.game.CreateGameRequest;
import com.stage.auth.authbackend.dto.game.UpdateGameRequest;
import com.stage.auth.authbackend.entity.EtatJeu;
import com.stage.auth.authbackend.entity.GameReviewAction;
import com.stage.auth.authbackend.entity.GameReviewHistory;
import com.stage.auth.authbackend.entity.Jeu;
import com.stage.auth.authbackend.entity.Role;
import com.stage.auth.authbackend.entity.TypeJeu;
import com.stage.auth.authbackend.entity.User;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.game.CarteMemoireRepository;
import com.stage.auth.authbackend.repository.game.GameReviewHistoryRepository;
import com.stage.auth.authbackend.repository.game.JeuRepository;
import com.stage.auth.authbackend.repository.game.ParametresReflexeRepository;
import com.stage.auth.authbackend.repository.game.PuzzleLogiqueRepository;
import com.stage.auth.authbackend.repository.game.QuestionRepository;
import com.stage.auth.authbackend.repository.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
     * Service éducateur : gestion complète des jeux.
 * L'admin crée les jeux ; l'éducateur gère le contenu par type.
 */
@Service
@RequiredArgsConstructor
public class EducatorGameService {

    private final JeuRepository jeuRepository;
    private final QuestionRepository questionRepository;
    private final CarteMemoireRepository carteMemoireRepository;
    private final PuzzleLogiqueRepository puzzleLogiqueRepository;
    private final ParametresReflexeRepository parametresReflexeRepository;
    private final GameReviewHistoryRepository gameReviewHistoryRepository;
    private final UserRepository userRepository;
    private final jakarta.persistence.EntityManager entityManager;

    public List<GameDTO> findAllGames() {
        return jeuRepository.findAll().stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
    }

    public GameDTO findGameById(Long id) {
        Jeu jeu = jeuRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        return toDTO(jeu);
    }

    public GameDTO createGame(CreateGameRequest request, String educatorEmail) {
        if (educatorEmail == null || educatorEmail.isBlank()) {
            throw ApiException.unauthorized("Utilisateur éducateur non authentifié");
        }
        User educator = userRepository.findByEmail(educatorEmail.trim().toLowerCase())
                .orElseThrow(() -> ApiException.unauthorized("Éducateur introuvable"));
        if (educator.getRole() != Role.EDUCATEUR) {
            throw ApiException.unauthorized("Seul un éducateur peut créer un jeu depuis cet endpoint");
        }

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
                .etat(EtatJeu.BROUILLON)
                .dateCreation(LocalDateTime.now())
                .educateur(educator)
                .build();
        jeu = jeuRepository.save(jeu);
        return toDTO(jeu);
    }

    public GameDTO updateGame(Long id, UpdateGameRequest request) {
        Jeu jeu = jeuRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        EducatorGameEditPolicy.requireDraft(jeu);
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

    public GameDTO submitGame(Long id) {
        Jeu jeu = jeuRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        EducatorGameEditPolicy.requireDraft(jeu);

        validateContentUpdatedAfterRefusal(jeu);
        validateGameContentBeforeSubmit(jeu);
        jeu.setEtat(EtatJeu.EN_ATTENTE);
        jeu = jeuRepository.save(jeu);
        return toDTO(jeu);
    }

    private void validateContentUpdatedAfterRefusal(Jeu jeu) {
        if (jeu.getEtat() != EtatJeu.REFUSE) {
            return;
        }
        GameReviewHistory lastRefusal = gameReviewHistoryRepository
                .findTopByJeuIdAndActionOrderByCreatedAtDescIdDesc(jeu.getId(), GameReviewAction.REFUSE)
                .orElse(null);
        if (lastRefusal == null) {
            return;
        }

        LocalDateTime lastContentUpdateAt = jeu.getLastContentUpdateAt();
        if (lastContentUpdateAt == null || !lastContentUpdateAt.isAfter(lastRefusal.getCreatedAt())) {
            throw ApiException.badRequest("Ce jeu a été refusé. Modifiez le contenu du jeu avant de le soumettre à nouveau.");
        }
    }

    private void validateGameContentBeforeSubmit(Jeu jeu) {
        // Validation de contenu selon le type de jeu.
        if (jeu.getTypeJeu() == TypeJeu.QUIZ) {
            if (questionRepository.findByJeuId(jeu.getId()).isEmpty()) {
                throw ApiException.badRequest("Ajoutez au moins une question avant de finaliser ce jeu");
            }
            return;
        }

        if (jeu.getTypeJeu() == TypeJeu.MEMOIRE) {
            if (carteMemoireRepository.findByJeuId(jeu.getId()).size() < 2) {
                throw ApiException.badRequest("Ajoutez au moins une paire de cartes avant de finaliser ce jeu");
            }
            return;
        }

        if (jeu.getTypeJeu() == TypeJeu.REFLEXE) {
            var reflex = parametresReflexeRepository.findByJeuId(jeu.getId()).orElse(null);
            if (reflex == null || reflex.getNombreRounds() == null || reflex.getNombreRounds() < 1) {
                throw ApiException.badRequest("Configurez les paramètres réflexe (au moins 1 round) avant de finaliser ce jeu");
            }
            return;
        }

        if (jeu.getTypeJeu() == TypeJeu.LOGIQUE) {
            if (puzzleLogiqueRepository.findByJeuId(jeu.getId()).isEmpty()) {
                throw ApiException.badRequest("Ajoutez au moins un puzzle logique avant de finaliser ce jeu");
            }
            return;
        }
    }

    @org.springframework.transaction.annotation.Transactional
    public void deleteGame(Long id) {
        Jeu jeu = jeuRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        // La suppression reste autorisée même après finalisation ; seules les modifications sont interdites.

        // Hibernate refuse la suppression d'un jeu lié car les relations ne sont pas en CascadeType.REMOVE ou Bidirectionnel complet.
        // On supprime proprement toutes les dépendances via EntityManager natif (plus léger et sécurisé pour un Hard Delete)

        // 1. Dépendances de SessionJeu (Niveau 2)
        entityManager.createQuery("DELETE FROM StatistiquesPerformance s WHERE s.sessionJeu.id IN (SELECT sj.id FROM SessionJeu sj WHERE sj.jeu.id = :id)")
                .setParameter("id", id).executeUpdate();
        entityManager.createQuery("DELETE FROM InteractionJeu i WHERE i.sessionJeu.id IN (SELECT sj.id FROM SessionJeu sj WHERE sj.jeu.id = :id)")
                .setParameter("id", id).executeUpdate();

        // 2. Dépendances directes de Jeu (Niveau 1)
        entityManager.createQuery("DELETE FROM SessionJeu sj WHERE sj.jeu.id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.createQuery("DELETE FROM EvenementReflexe e WHERE e.jeu.id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.createQuery("DELETE FROM ParametresReflexe p WHERE p.jeu.id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.createQuery("DELETE FROM PuzzleLogique p WHERE p.jeu.id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.createQuery("DELETE FROM CarteMemoire c WHERE c.jeu.id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.createQuery("DELETE FROM Question q WHERE q.jeu.id = :id")
                .setParameter("id", id).executeUpdate();
        entityManager.createQuery("DELETE FROM Tournoi t WHERE t.jeu.id = :id")
                .setParameter("id", id).executeUpdate();

        // 3. Suppression du jeu lui-même
        jeuRepository.delete(jeu);
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
