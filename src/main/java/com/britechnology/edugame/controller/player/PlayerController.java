package com.britechnology.edugame.controller.player;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.britechnology.edugame.dto.educator.LogicPuzzleDTO;
import com.britechnology.edugame.dto.educator.MemoryCardDTO;
import com.britechnology.edugame.dto.educator.QuizQuestionDTO;
import com.britechnology.edugame.dto.educator.ReflexSettingsDTO;
import com.britechnology.edugame.dto.game.GameDTO;
import com.britechnology.edugame.dto.player.ChangePasswordRequest;
import com.britechnology.edugame.dto.player.CreateRoomRequest;
import com.britechnology.edugame.dto.player.LinkedChildProfileDTO;
import com.britechnology.edugame.dto.player.CreateGameSessionRequest;
import com.britechnology.edugame.dto.player.CreateGameSessionResponse;
import com.britechnology.edugame.dto.player.JoinRoomRequest;
import com.britechnology.edugame.dto.player.PlayerBadgeOverviewItemDTO;
import com.britechnology.edugame.dto.player.PlayerBadgesOverviewDTO;
import com.britechnology.edugame.dto.player.PlayerHistorySessionDTO;
import com.britechnology.edugame.dto.player.PlayerOnboardingRequest;
import com.britechnology.edugame.dto.player.PlayerProgressOverviewDTO;
import com.britechnology.edugame.dto.player.PlayerRewardOverviewItemDTO;
import com.britechnology.edugame.dto.player.PlayerRewardsOverviewDTO;
import com.britechnology.edugame.dto.player.RealtimeRoomStateDTO;
import com.britechnology.edugame.dto.player.RoomReadyMessage;
import com.britechnology.edugame.dto.player.SoloLeaderboardEntryDTO;
import com.britechnology.edugame.dto.player.CompetitiveRoomResultDTO;
import com.britechnology.edugame.dto.player.UpdateProfileRequest;
import com.britechnology.edugame.dto.player.UserDTO;
import com.britechnology.edugame.dto.reclamation.CreateReclamationRequest;
import com.britechnology.edugame.dto.reclamation.ReclamationDTO;
import com.britechnology.edugame.entity.CarteMemoire;
import com.britechnology.edugame.entity.EtatJeu;
import com.britechnology.edugame.entity.Jeu;
import com.britechnology.edugame.entity.PuzzleLogique;
import com.britechnology.edugame.entity.Question;
import com.britechnology.edugame.entity.TypeJeu;
import com.britechnology.edugame.entity.User;
import com.britechnology.edugame.service.quiz.QuizVariantSupport;
import com.britechnology.edugame.util.AvatarPolicy;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.game.CarteMemoireRepository;
import com.britechnology.edugame.repository.game.JeuRepository;
import com.britechnology.edugame.repository.game.PuzzleLogiqueRepository;
import com.britechnology.edugame.repository.game.QuestionRepository;
import com.britechnology.edugame.repository.badge.NiveauRepository;
import com.britechnology.edugame.repository.user.UserRepository;
import com.britechnology.edugame.service.educator.EducatorReflexService;
import com.britechnology.edugame.service.player.ParentLinkageService;
import com.britechnology.edugame.service.player.PlayerService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import jakarta.validation.Valid;

import java.util.Map;

import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class PlayerController {

    private final UserRepository userRepository;
    private final PlayerService playerService;
    private final ParentLinkageService parentLinkageService;
    private final EducatorReflexService educatorReflexService;
    private final JeuRepository jeuRepository;
    private final QuestionRepository questionRepository;
    private final CarteMemoireRepository carteMemoireRepository;
    private final PuzzleLogiqueRepository puzzleLogiqueRepository;
    private final ObjectMapper objectMapper;
    private final NiveauRepository niveauRepository;

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
                .avatarUrl(AvatarPolicy.publicAvatarUrl(user))
                .role(user.getRole().name())
                .etatCompte(user.getEtatCompte())
                .enabled(user.isEnabled())
                .dateDeNaissance(user.getDateDeNaissance())
                .niveau(user.getNiveau())
                .scoreTotal(user.getScoreTotal())
                .pointsExperience(user.getPointsExperience())
                .xpToNextLevel(xpToNextLevel(user.getNiveau() != null ? Math.max(1, user.getNiveau()) : 1))
                .currentStreakDays(user.getCurrentStreakDays())
                .bestStreakDays(user.getBestStreakDays())
                .lastStreakDate(user.getLastStreakDate())
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
                .idParent(user.getParent() != null ? user.getParent().getId() : null)
                .build();
    }

    @GetMapping("/me/linked-children")
    public ResponseEntity<java.util.List<LinkedChildProfileDTO>> getLinkedChildren(Authentication authentication) {
        return ResponseEntity.ok(parentLinkageService.getLinkedChildren(authentication));
    }

    @GetMapping("/me/linked-children/{childId}/history")
    public ResponseEntity<java.util.List<PlayerHistorySessionDTO>> getLinkedChildHistory(
            Authentication authentication,
            @PathVariable Long childId
    ) {
        return ResponseEntity.ok(parentLinkageService.getLinkedChildHistory(authentication, childId));
    }

    @GetMapping("/me/linked-children/{childId}/badges")
    public ResponseEntity<java.util.List<PlayerBadgeOverviewItemDTO>> getLinkedChildBadges(
            Authentication authentication,
            @PathVariable Long childId
    ) {
        return ResponseEntity.ok(parentLinkageService.getLinkedChildBadges(authentication, childId));
    }

    @PutMapping("/update-profile")
    public UserDTO updateProfile(Authentication authentication, @RequestBody UpdateProfileRequest request) {
        return playerService.updateProfile(authentication, request);
    }

    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(Authentication authentication, @RequestBody ChangePasswordRequest request) {
        playerService.changePassword(authentication, request);
        return ResponseEntity.ok(Map.of("message", "Mot de passe changé"));
    }

    @PatchMapping("/me/onboarding")
    public UserDTO completeOnboarding(
            Authentication authentication,
            @RequestBody(required = true) PlayerOnboardingRequest request
    ) {
        return playerService.completeOnboarding(authentication, request);
    }

    @PostMapping("/game-sessions")
    public ResponseEntity<CreateGameSessionResponse> createGameSession(
            Authentication authentication,
            @RequestBody CreateGameSessionRequest request
    ) {
        return ResponseEntity.ok(playerService.createGameSession(authentication, request));
    }

    @PostMapping("/reclamations")
    public ResponseEntity<ReclamationDTO> createReclamation(
            Authentication authentication,
            @Valid @RequestBody CreateReclamationRequest request
    ) {
        return ResponseEntity.status(201).body(playerService.createReclamation(authentication, request));
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

    @GetMapping("/games/{gameId}/logic-puzzles")
    public ResponseEntity<java.util.List<LogicPuzzleDTO>> listLogicPuzzlesByGame(@PathVariable Long gameId) {
        Jeu jeu = jeuRepository.findById(gameId)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        if (jeu.getTypeJeu() != TypeJeu.LOGIQUE) {
            throw ApiException.badRequest("Ce jeu n'est pas de type LOGIQUE");
        }
        if (jeu.getEtat() != EtatJeu.ACCEPTE || !jeu.isActif()) {
            return ResponseEntity.noContent().build();
        }
        java.util.List<LogicPuzzleDTO> rows = puzzleLogiqueRepository.findByJeuId(gameId).stream()
                .map(p -> toLogicPuzzleDTO(p, jeu))
                .toList();
        return ResponseEntity.ok(rows);
    }

    @GetMapping("/leaderboard/solo")
    public ResponseEntity<java.util.List<SoloLeaderboardEntryDTO>> getSoloLeaderboard() {
        return ResponseEntity.ok(playerService.getSoloLeaderboard());
    }

    @GetMapping("/leaderboard/solo/scoped")
    public ResponseEntity<com.britechnology.edugame.dto.player.SoloLeaderboardResponseDTO> getSoloLeaderboardScoped(
            Authentication authentication,
            @RequestParam(defaultValue = "GLOBAL") String scope
    ) {
        return ResponseEntity.ok(playerService.getSoloLeaderboardScoped(authentication, scope));
    }

    @GetMapping("/leaderboard/me/ranks")
    public ResponseEntity<com.britechnology.edugame.dto.player.PlayerLeaderboardRanksDTO> getMyLeaderboardRanks(
            Authentication authentication
    ) {
        return ResponseEntity.ok(playerService.getMyLeaderboardRanks(authentication));
    }

    @GetMapping("/progress/overview")
    public ResponseEntity<PlayerProgressOverviewDTO> getProgressOverview(Authentication authentication) {
        return ResponseEntity.ok(playerService.getProgressOverview(authentication));
    }

    @GetMapping("/badges/overview")
    public ResponseEntity<PlayerBadgesOverviewDTO> getBadgesOverview(Authentication authentication) {
        return ResponseEntity.ok(playerService.getBadgesOverview(authentication));
    }

    @PostMapping("/badges/{badgeId}/claim")
    public ResponseEntity<PlayerBadgeOverviewItemDTO> claimBadge(
            Authentication authentication,
            @PathVariable Long badgeId
    ) {
        return ResponseEntity.ok(playerService.claimBadge(authentication, badgeId));
    }

    @GetMapping("/history/sessions")
    public ResponseEntity<java.util.List<PlayerHistorySessionDTO>> getHistorySessions(Authentication authentication) {
        return ResponseEntity.ok(playerService.getHistorySessions(authentication));
    }

    @GetMapping("/rewards/overview")
    public ResponseEntity<PlayerRewardsOverviewDTO> getRewardsOverview(Authentication authentication) {
        return ResponseEntity.ok(playerService.getRewardsOverview(authentication));
    }

    @PostMapping("/rewards/{rewardId}/claim")
    public ResponseEntity<PlayerRewardOverviewItemDTO> claimReward(
            Authentication authentication,
            @PathVariable Long rewardId
    ) {
        return ResponseEntity.ok(playerService.claimReward(authentication, rewardId));
    }

    @PostMapping("/rooms/create")
    public ResponseEntity<RealtimeRoomStateDTO> createRoom(
            Authentication authentication,
            @RequestBody CreateRoomRequest request
    ) {
        return ResponseEntity.ok(playerService.createRealtimeRoom(authentication, request));
    }

    @PostMapping("/rooms/join")
    public ResponseEntity<RealtimeRoomStateDTO> joinRoom(
            Authentication authentication,
            @RequestBody JoinRoomRequest request
    ) {
        return ResponseEntity.ok(playerService.joinRealtimeRoom(authentication, request));
    }

    @GetMapping("/rooms/{roomCode}")
    public ResponseEntity<RealtimeRoomStateDTO> getRoom(
            @PathVariable String roomCode
    ) {
        return ResponseEntity.ok(playerService.getRealtimeRoom(roomCode));
    }

    @GetMapping("/rooms/{roomCode}/result")
    public ResponseEntity<CompetitiveRoomResultDTO> getRoomResult(
            Authentication authentication,
            @PathVariable String roomCode,
            @RequestParam Long gameId
    ) {
        return ResponseEntity.ok(
                playerService.getCompetitiveRoomResult(authentication, roomCode, gameId)
        );
    }

    @PatchMapping("/rooms/{roomCode}/ready")
    public ResponseEntity<RealtimeRoomStateDTO> setRoomReady(
            Authentication authentication,
            @PathVariable String roomCode,
            @RequestBody RoomReadyMessage request
    ) {
        boolean ready = request != null && Boolean.TRUE.equals(request.getReady());
        return ResponseEntity.ok(playerService.setRealtimeRoomReady(authentication, roomCode, ready));
    }

    @PostMapping("/rooms/{roomCode}/start")
    public ResponseEntity<RealtimeRoomStateDTO> startRoom(
            Authentication authentication,
            @PathVariable String roomCode
    ) {
        return ResponseEntity.ok(playerService.startRealtimeRoom(authentication, roomCode));
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
                .quizPlayMode(jeu.getQuizPlayMode())
                .quizVariant(jeu.getQuizVariant())
                .actif(jeu.isActif())
                .dureeMinutes(jeu.getDureeMinutes())
                .icone(jeu.getIcone())
                .coverImageUrl(jeu.getCoverImageUrl())
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
                .sousType(QuizVariantSupport.resolveCode(jeu.getQuizVariant(), question))
                .mediaUrl(question.getMediaUrl())
                .promptAudioUrl(question.getPromptAudioUrl())
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
                .cardType(card.getCardType())
                .cardValue(card.getCardValue())
                .sousType(card.getSousType())
                .pairKey(card.getPairKey())
                .categorie(card.getCategorie())
                .build();
    }

    private LogicPuzzleDTO toLogicPuzzleDTO(PuzzleLogique puzzle, Jeu jeu) {
        return LogicPuzzleDTO.builder()
                .id(puzzle.getId())
                .jeuId(jeu.getId())
                .jeuTitre(jeu.getTitre())
                .enonce(puzzle.getEnonce())
                .sousType(puzzle.getSousType())
                .donnees(puzzle.getDonnees())
                .bonneReponse(puzzle.getBonneReponse())
                .indice(puzzle.getIndice())
                .difficulte(puzzle.getDifficulte())
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

    private int xpToNextLevel(int level) {
        return niveauRepository.findByNiveau(level)
                .map(cfg -> Math.max(1, cfg.getPointMin() != null ? cfg.getPointMin() : 0))
                .orElse(Math.max(250, (level * 150) + (level * level * 55)));
    }
}
