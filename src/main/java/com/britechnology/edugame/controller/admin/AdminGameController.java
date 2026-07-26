package com.britechnology.edugame.controller.admin;

import com.britechnology.edugame.dto.educator.LogicPuzzleDTO;
import com.britechnology.edugame.dto.educator.MemoryCardDTO;
import com.britechnology.edugame.dto.educator.QuizQuestionDTO;
import com.britechnology.edugame.dto.educator.ReflexSettingsDTO;
import com.britechnology.edugame.service.admin.AiGameReviewService;
import com.britechnology.edugame.service.educator.EducatorLogicService;
import com.britechnology.edugame.service.educator.EducatorMemoryService;
import com.britechnology.edugame.service.educator.EducatorQuestionService;
import com.britechnology.edugame.service.educator.EducatorReflexService;
import com.britechnology.edugame.dto.game.CreateGameRequest;
import com.britechnology.edugame.dto.game.ChangeGameStatusRequest;
import com.britechnology.edugame.dto.game.GameAiReviewDTO;
import com.britechnology.edugame.dto.game.GameDTO;
import com.britechnology.edugame.dto.game.UpdateGameRequest;
import com.britechnology.edugame.service.admin.AdminGameService;
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
    private final AiGameReviewService aiGameReviewService;
    private final EducatorQuestionService educatorQuestionService;
    private final EducatorMemoryService educatorMemoryService;
    private final EducatorLogicService educatorLogicService;
    private final EducatorReflexService educatorReflexService;

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
    public ResponseEntity<List<QuizQuestionDTO>> getQuestionsByGameId(@PathVariable Long id) {
        return ResponseEntity.ok(educatorQuestionService.listByGame(id));
    }

    /**
     * GET /api/admin/games/{id}/memory-cards
     * Liste les cartes mémoire du jeu (type MEMOIRE) pour l'admin.
     */
    @GetMapping("/{id}/memory-cards")
    public ResponseEntity<List<MemoryCardDTO>> getMemoryCardsByGameId(@PathVariable Long id) {
        return ResponseEntity.ok(educatorMemoryService.listByGame(id));
    }

    /**
     * GET /api/admin/games/{id}/logic-puzzles
     * Liste les puzzles logiques du jeu (type LOGIQUE) pour l'admin.
     */
    @GetMapping("/{id}/logic-puzzles")
    public ResponseEntity<List<LogicPuzzleDTO>> getLogicPuzzlesByGameId(@PathVariable Long id) {
        return ResponseEntity.ok(educatorLogicService.listByGame(id));
    }

    /**
     * GET /api/admin/games/{id}/reflex-settings
     * Récupère les paramètres réflexe du jeu (type REFLEXE) pour l'admin.
     */
    @GetMapping("/{id}/reflex-settings")
    public ResponseEntity<ReflexSettingsDTO> getReflexSettingsByGameId(@PathVariable Long id) {
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
