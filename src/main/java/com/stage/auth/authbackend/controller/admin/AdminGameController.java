package com.stage.auth.authbackend.controller.admin;

import com.stage.auth.authbackend.dto.game.CreateGameRequest;
import com.stage.auth.authbackend.dto.game.ChangeGameStatusRequest;
import com.stage.auth.authbackend.dto.game.GameAiReviewDTO;
import com.stage.auth.authbackend.dto.game.GameDTO;
import com.stage.auth.authbackend.dto.game.UpdateGameRequest;
import com.stage.auth.authbackend.service.admin.AdminGameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * API admin pour la gestion des jeux (Manage Games).
 * Toutes les routes nécessitent le rôle ADMIN.
 */
@RestController
@RequestMapping("/api/admin/games")
@RequiredArgsConstructor
public class AdminGameController {

    private final AdminGameService adminGameService;
    private final com.stage.auth.authbackend.service.admin.AiGameReviewService aiGameReviewService;
    private final com.stage.auth.authbackend.service.educator.EducatorQuestionService educatorQuestionService;
    private final com.stage.auth.authbackend.service.educator.EducatorMemoryService educatorMemoryService;
    private final com.stage.auth.authbackend.service.educator.EducatorLogicService educatorLogicService;
    private final com.stage.auth.authbackend.service.educator.EducatorReflexService educatorReflexService;

    /**
     * GET /api/admin/games
     * Liste tous les jeux.
     */
    @GetMapping
    public ResponseEntity<List<GameDTO>> getAllGames() {
        return ResponseEntity.ok(adminGameService.findAllGames());
    }

    /**
     * GET /api/admin/games/{id}
     * Détail d'un jeu.
     */
    @GetMapping("/{id}")
    public ResponseEntity<GameDTO> getGameById(@PathVariable Long id) {
        return ResponseEntity.ok(adminGameService.findGameById(id));
    }

    @GetMapping("/{id}/ai-review")
    public ResponseEntity<GameAiReviewDTO> getAiReview(@PathVariable Long id) {
        return ResponseEntity.ok(aiGameReviewService.reviewGame(id));
    }

    /**
     * GET /api/admin/games/{id}/questions
     * Liste les questions du jeu (type QUIZ) pour l'admin.
     */
    @GetMapping("/{id}/questions")
    public ResponseEntity<List<com.stage.auth.authbackend.dto.educator.QuizQuestionDTO>> getQuestionsByGameId(@PathVariable Long id) {
        return ResponseEntity.ok(educatorQuestionService.listByGame(id));
    }

    /**
     * GET /api/admin/games/{id}/memory-cards
     * Liste les cartes mémoire du jeu (type MEMOIRE) pour l'admin.
     */
    @GetMapping("/{id}/memory-cards")
    public ResponseEntity<List<com.stage.auth.authbackend.dto.educator.MemoryCardDTO>> getMemoryCardsByGameId(@PathVariable Long id) {
        return ResponseEntity.ok(educatorMemoryService.listByGame(id));
    }

    /**
     * GET /api/admin/games/{id}/logic-puzzles
     * Liste les puzzles logiques du jeu (type LOGIQUE) pour l'admin.
     */
    @GetMapping("/{id}/logic-puzzles")
    public ResponseEntity<List<com.stage.auth.authbackend.dto.educator.LogicPuzzleDTO>> getLogicPuzzlesByGameId(@PathVariable Long id) {
        return ResponseEntity.ok(educatorLogicService.listByGame(id));
    }

    /**
     * GET /api/admin/games/{id}/reflex-settings
     * Récupère les paramètres réflexe du jeu (type REFLEXE) pour l'admin.
     */
    @GetMapping("/{id}/reflex-settings")
    public ResponseEntity<com.stage.auth.authbackend.dto.educator.ReflexSettingsDTO> getReflexSettingsByGameId(@PathVariable Long id) {
        return ResponseEntity.ok(educatorReflexService.getByGame(id));
    }

    /**
     * POST /api/admin/games
     * Crée un nouveau jeu.
     */
    @PostMapping
    public ResponseEntity<GameDTO> createGame(@Valid @RequestBody CreateGameRequest request) {
        GameDTO game = adminGameService.createGame(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(game);
    }

    /**
     * PUT /api/admin/games/{id}
     * Met à jour un jeu.
     */
    @PutMapping("/{id}")
    public ResponseEntity<GameDTO> updateGame(@PathVariable Long id, @Valid @RequestBody UpdateGameRequest request) {
        return ResponseEntity.ok(adminGameService.updateGame(id, request));
    }

    /**
     * DELETE /api/admin/games/{id}
     * Supprime un jeu.
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable Long id) {
        adminGameService.deleteGame(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /api/admin/games/{id}/status
     * Met à jour l'état d'un jeu
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<GameDTO> changeGameState(
            @PathVariable Long id,
            @Valid @RequestBody ChangeGameStatusRequest request,
            Authentication authentication
    ) {
        String adminEmail = authentication != null ? authentication.getName() : null;
        return ResponseEntity.ok(adminGameService.changeGameState(id, request.getEtat(), request.getMotifRefus(), adminEmail));
    }
}
