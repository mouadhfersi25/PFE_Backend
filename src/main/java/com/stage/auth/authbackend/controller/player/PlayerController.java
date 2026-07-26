package com.stage.auth.authbackend.controller.player;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.stage.auth.authbackend.dto.educator.LogicPuzzleDTO;
import com.stage.auth.authbackend.dto.educator.MemoryCardDTO;
import com.stage.auth.authbackend.dto.educator.QuizQuestionDTO;
import com.stage.auth.authbackend.dto.educator.ReflexSettingsDTO;
import com.stage.auth.authbackend.dto.game.GameDTO;
import com.stage.auth.authbackend.dto.player.ChangePasswordRequest;
import com.stage.auth.authbackend.dto.player.CreateRoomRequest;
import com.stage.auth.authbackend.dto.player.CreateGameSessionRequest;
import com.stage.auth.authbackend.dto.player.CreateGameSessionResponse;
import com.stage.auth.authbackend.dto.player.JoinRoomRequest;
import com.stage.auth.authbackend.dto.player.PlayerBadgeOverviewItemDTO;
import com.stage.auth.authbackend.dto.player.PlayerBadgesOverviewDTO;
import com.stage.auth.authbackend.dto.player.PlayerHistorySessionDTO;
import com.stage.auth.authbackend.dto.player.PlayerOnboardingRequest;
import com.stage.auth.authbackend.dto.player.PlayerProgressOverviewDTO;
import com.stage.auth.authbackend.dto.player.PlayerRewardOverviewItemDTO;
import com.stage.auth.authbackend.dto.player.PlayerRewardsOverviewDTO;
import com.stage.auth.authbackend.dto.player.RealtimeRoomStateDTO;
import com.stage.auth.authbackend.dto.player.RoomReadyMessage;
import com.stage.auth.authbackend.dto.player.RoomTeamNameMessage;
import com.stage.auth.authbackend.dto.player.SoloLeaderboardEntryDTO;
import com.stage.auth.authbackend.dto.player.TeamLeaderboardEntryDTO;
import com.stage.auth.authbackend.dto.player.UpdateProfileRequest;
import com.stage.auth.authbackend.dto.player.UserDTO;
import com.stage.auth.authbackend.entity.CarteMemoire;
import com.stage.auth.authbackend.entity.EtatJeu;
import com.stage.auth.authbackend.entity.Jeu;
import com.stage.auth.authbackend.entity.PuzzleLogique;
import com.stage.auth.authbackend.entity.Question;
import com.stage.auth.authbackend.entity.TypeJeu;
import com.stage.auth.authbackend.entity.User;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.game.CarteMemoireRepository;
import com.stage.auth.authbackend.repository.game.JeuRepository;
import com.stage.auth.authbackend.repository.game.PuzzleLogiqueRepository;
import com.stage.auth.authbackend.repository.game.QuestionRepository;
import com.stage.auth.authbackend.repository.badge.NiveauRepository;
import com.stage.auth.authbackend.repository.user.UserRepository;
import com.stage.auth.authbackend.service.educator.EducatorReflexService;
import com.stage.auth.authbackend.service.player.PlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class PlayerController {

    private final UserRepository userRepository;
    private final PlayerService playerService;
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
                .avatarUrl(user.getAvatarUrl())
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
                .build();
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

    @GetMapping("/leaderboard/team")
    public ResponseEntity<java.util.List<TeamLeaderboardEntryDTO>> getTeamLeaderboard() {
        return ResponseEntity.ok(playerService.getTeamLeaderboard());
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

    @PatchMapping("/rooms/{roomCode}/team-name")
    public ResponseEntity<RealtimeRoomStateDTO> setTeamName(
            Authentication authentication,
            @PathVariable String roomCode,
            @RequestBody RoomTeamNameMessage request
    ) {
        return ResponseEntity.ok(playerService.updateRealtimeRoomTeamName(authentication, roomCode, request != null ? request.getTeamName() : null));
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
                .sousType(question.getSousType())
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
