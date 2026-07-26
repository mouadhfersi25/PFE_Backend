package com.stage.auth.authbackend.controller.user;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stage.auth.authbackend.dto.educator.ReflexSettingsDTO;
import com.stage.auth.authbackend.dto.educator.QuizQuestionDTO;
import com.stage.auth.authbackend.dto.educator.MemoryCardDTO;
import com.stage.auth.authbackend.dto.game.GameDTO;
import com.stage.auth.authbackend.dto.user.ChangePasswordRequest;
import com.stage.auth.authbackend.dto.user.PlayerOnboardingRequest;
import com.stage.auth.authbackend.dto.user.UpdateProfileRequest;
import com.stage.auth.authbackend.dto.user.UserDTO;
import com.stage.auth.authbackend.entity.EtatJeu;
import com.stage.auth.authbackend.entity.CarteMemoire;
import com.stage.auth.authbackend.entity.Jeu;
import com.stage.auth.authbackend.entity.Question;
import com.stage.auth.authbackend.entity.TypeJeu;
import com.stage.auth.authbackend.entity.User;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.game.JeuRepository;
import com.stage.auth.authbackend.repository.game.CarteMemoireRepository;
import com.stage.auth.authbackend.repository.game.QuestionRepository;
import com.stage.auth.authbackend.repository.user.UserRepository;
import com.stage.auth.authbackend.service.educator.EducatorReflexService;
import com.stage.auth.authbackend.service.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserRepository userRepository;
    private final UserService userService;
    private final EducatorReflexService educatorReflexService;
    private final JeuRepository jeuRepository;
    private final QuestionRepository questionRepository;
    private final CarteMemoireRepository carteMemoireRepository;
    private final ObjectMapper objectMapper;

    @GetMapping("/me")
    public UserDTO getMe(Authentication authentication) {

        String email = authentication.getName();

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> ApiException.notFound("Utilisateur introuvable"));

        return UserDTO.builder()
                .id(user.getId())
                .nom(user.getNom())
                .prenom(user.getPrenom())
                .email(user.getEmail())
                .password(null)
                .telephone(user.getTelephone())
                .avatarUrl(user.getAvatarUrl())
                .role(user.getRole().name())
                .etatCompte(user.getEtatCompte())
                .enabled(user.isEnabled())
                .dateDeNaissance(user.getDateDeNaissance())
                .niveau(user.getNiveau())
                .scoreTotal(user.getScoreTotal())
                .pointsExperience(user.getPointsExperience())
                .idRegion(user.getRegion() != null ? user.getRegion().getId() : null)
                .regionNom(user.getRegion() != null ? user.getRegion().getNom() : null)
                .idPays(user.getRegion() != null && user.getRegion().getPays() != null ? user.getRegion().getPays().getId() : null)
                .paysNom(user.getRegion() != null && user.getRegion().getPays() != null ? user.getRegion().getPays().getNom() : null)
                .onboardingCompleted(user.isOnboardingCompleted())
                .idGenre(user.getGenre() != null ? user.getGenre().getId() : null)
                .resetToken(user.getResetToken())
                .resetTokenExpiry(user.getResetTokenExpiry())
                .tokenVerification(user.getTokenVerification())
                .dateExpirationToken(user.getDateExpirationToken())
                .dateDerniereConnexion(user.getDateDerniereConnexion())
                .dateCreation(user.getDateCreation())
                .build();
    }

    @PutMapping("/update-profile")
    public UserDTO updateProfile(Authentication authentication,
                                 @RequestBody UpdateProfileRequest request) {
        return userService.updateProfile(authentication, request);
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(
            Authentication authentication,
            @RequestBody ChangePasswordRequest request
    ) {
        userService.changePassword(authentication, request);
        return ResponseEntity.ok(Map.of("message", "Mot de passe changé"));
    }

    @PatchMapping("/me/onboarding")
    public UserDTO completeOnboarding(Authentication authentication,
                                      @RequestBody(required = true) PlayerOnboardingRequest request) {
        return userService.completeOnboarding(authentication, request);
    }

    @GetMapping("/games/{gameId}/reflex-settings")
    public ResponseEntity<ReflexSettingsDTO> getReflexSettingsForGame(@PathVariable Long gameId) {
        Jeu jeu = jeuRepository.findById(gameId)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        if (jeu.getEtat() != EtatJeu.ACCEPTE || !jeu.isActif()) {
            return ResponseEntity.noContent().build();
        }
        ReflexSettingsDTO dto = educatorReflexService.getByGame(gameId);
        return dto != null ? ResponseEntity.ok(dto) : ResponseEntity.noContent().build();
    }

    @GetMapping("/games/available")
    public ResponseEntity<java.util.List<GameDTO>> listAvailableGames() {
        java.util.List<GameDTO> games = jeuRepository.findAll().stream()
                .filter(j -> j.getEtat() == EtatJeu.ACCEPTE && j.isActif())
                .map(this::toGameDTO)
                .toList();
        return ResponseEntity.ok(games);
    }

    @GetMapping("/games/{gameId}/quiz-questions")
    public ResponseEntity<java.util.List<QuizQuestionDTO>> listQuizQuestionsByGame(@PathVariable Long gameId) {
        Jeu jeu = jeuRepository.findById(gameId)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        if (jeu.getTypeJeu() != TypeJeu.QUIZ) {
            throw ApiException.badRequest("Ce jeu n'est pas de type QUIZ");
        }
        if (jeu.getEtat() != EtatJeu.ACCEPTE || !jeu.isActif()) {
            return ResponseEntity.noContent().build();
        }
        java.util.List<QuizQuestionDTO> rows = questionRepository.findByJeuId(gameId).stream()
                .map(q -> toQuizQuestionDTO(q, jeu))
                .toList();
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/games/{gameId}/memory-cards")
    public ResponseEntity<java.util.List<MemoryCardDTO>> listMemoryCardsByGame(@PathVariable Long gameId) {
        Jeu jeu = jeuRepository.findById(gameId)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        if (jeu.getTypeJeu() != TypeJeu.MEMOIRE) {
            throw ApiException.badRequest("Ce jeu n'est pas de type MEMOIRE");
        }
        if (jeu.getEtat() != EtatJeu.ACCEPTE || !jeu.isActif()) {
            return ResponseEntity.noContent().build();
        }
        java.util.List<MemoryCardDTO> rows = carteMemoireRepository.findByJeuId(gameId).stream()
                .map(c -> toMemoryCardDTO(c, jeu))
                .toList();
        return ResponseEntity.ok(rows);
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
                .actif(jeu.isActif())
                .dureeMinutes(jeu.getDureeMinutes())
                .icone(jeu.getIcone())
                .etat(jeu.getEtat())
                .dateCreation(jeu.getDateCreation())
                .build();
    }

    private QuizQuestionDTO toQuizQuestionDTO(Question question, Jeu jeu) {
        return QuizQuestionDTO.builder()
                .id(question.getId())
                .jeuId(jeu.getId())
                .jeuTitre(jeu.getTitre())
                .contenu(question.getContenu())
                .bonneReponse(question.getBonneReponse())
                .options(jsonToOptions(question.getOptions()))
                .explication(question.getExplication())
                .difficulte(question.getDifficulte())
                .build();
    }

    private MemoryCardDTO toMemoryCardDTO(CarteMemoire card, Jeu jeu) {
        return MemoryCardDTO.builder()
                .id(card.getId())
                .jeuId(jeu.getId())
                .jeuTitre(jeu.getTitre())
                .symbole(card.getSymbole())
                .pairKey(card.getPairKey())
                .categorie(card.getCategorie())
                .build();
    }

    private java.util.List<String> jsonToOptions(String json) {
        if (json == null || json.isBlank()) return java.util.Collections.emptyList();
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return java.util.Collections.emptyList();
        }
    }
}
