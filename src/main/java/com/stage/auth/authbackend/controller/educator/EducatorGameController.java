package com.stage.auth.authbackend.controller.educator;

import com.stage.auth.authbackend.dto.game.CreateGameRequest;
import com.stage.auth.authbackend.dto.game.GameDTO;
import com.stage.auth.authbackend.dto.game.GenerateCoverPreviewRequest;
import com.stage.auth.authbackend.dto.game.UpdateGameRequest;
import com.stage.auth.authbackend.entity.Jeu;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.game.JeuRepository;
import com.stage.auth.authbackend.service.educator.GameCoverService;
import com.stage.auth.authbackend.service.educator.EducatorGameService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.client.RestTemplate;
import java.util.List;
import java.util.Map;
import java.util.Base64;
import java.net.URI;

/**
 * API éducateur : gestion des jeux.
 * GET /api/educator/games -> GameDTO[]
 * GET /api/educator/games/{id} -> GameDTO
 * POST /api/educator/games -> GameDTO
 * PUT /api/educator/games/{id} -> GameDTO
 * DELETE /api/educator/games/{id} -> Void
 */
@RestController
@RequestMapping("/api/educator/games")
@RequiredArgsConstructor
public class EducatorGameController {

    private final EducatorGameService educatorGameService;
    private final GameCoverService gameCoverService;
    private final JeuRepository jeuRepository;
    private final RestTemplate restTemplate = new RestTemplate();

    @GetMapping
    public ResponseEntity<List<GameDTO>> listGames() {
        return ResponseEntity.ok(educatorGameService.findAllGames());
    }

    @GetMapping("/{id}")
    public ResponseEntity<GameDTO> getGameById(@PathVariable Long id) {
        return ResponseEntity.ok(educatorGameService.findGameById(id));
    }

    @PostMapping
    public ResponseEntity<GameDTO> createGame(@Valid @RequestBody CreateGameRequest request, Authentication authentication) {
        String educatorEmail = authentication != null ? authentication.getName() : null;
        GameDTO game = educatorGameService.createGame(request, educatorEmail);
        return ResponseEntity.status(HttpStatus.CREATED).body(game);
    }

    @PutMapping("/{id}")
    public ResponseEntity<GameDTO> updateGame(@PathVariable Long id, @Valid @RequestBody UpdateGameRequest request) {
        return ResponseEntity.ok(educatorGameService.updateGame(id, request));
    }

    @PatchMapping("/{id}/submit")
    public ResponseEntity<GameDTO> submitGame(@PathVariable Long id) {
        return ResponseEntity.ok(educatorGameService.submitGame(id));
    }

    @PostMapping("/{id}/cover/generate")
    public ResponseEntity<GameDTO> generateCover(@PathVariable Long id) {
        Jeu jeu = jeuRepository.findById(id).orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        String coverUrl = gameCoverService.generateCoverUrl(jeu);
        UpdateGameRequest request = new UpdateGameRequest();
        request.setCoverImageUrl(coverUrl);
        return ResponseEntity.ok(educatorGameService.updateGame(id, request));
    }

    @PostMapping("/cover/generate-preview")
    public ResponseEntity<Map<String, String>> generateCoverPreview(@RequestBody GenerateCoverPreviewRequest request) {
        String coverUrl = gameCoverService.generateCoverUrlForPreview(request);
        return ResponseEntity.ok(Map.of("coverImageUrl", coverUrl));
    }

    @PostMapping("/{id}/cover/upload")
    public ResponseEntity<GameDTO> uploadCover(@PathVariable Long id, @RequestParam("file") MultipartFile file) {
        if (file == null || file.isEmpty()) throw ApiException.badRequest("Fichier cover requis");
        String contentType = file.getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw ApiException.badRequest("Le fichier doit être une image");
        }
        try {
            byte[] bytes = file.getBytes();
            if (bytes.length > 5 * 1024 * 1024) throw ApiException.badRequest("Image trop volumineuse (max 5MB)");
            String dataUrl = "data:" + contentType + ";base64," + Base64.getEncoder().encodeToString(bytes);
            UpdateGameRequest request = new UpdateGameRequest();
            request.setCoverImageUrl(dataUrl);
            return ResponseEntity.ok(educatorGameService.updateGame(id, request));
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.internalServerError("Impossible de traiter l'image uploadée");
        }
    }

    @DeleteMapping("/{id}/cover")
    public ResponseEntity<GameDTO> deleteCover(@PathVariable Long id) {
        UpdateGameRequest request = new UpdateGameRequest();
        request.setCoverImageUrl("");
        return ResponseEntity.ok(educatorGameService.updateGame(id, request));
    }

    @GetMapping("/{id}/cover-proxy")
    public ResponseEntity<byte[]> proxyCoverByGame(@PathVariable Long id) {
        Jeu jeu = jeuRepository.findById(id).orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        String url = jeu.getCoverImageUrl();
        if (url == null || url.isBlank()) throw ApiException.badRequest("Aucune cover disponible");
        if (url.startsWith("data:image/")) throw ApiException.badRequest("Cover locale non proxyable");
        try {
            URI uri = URI.create(url);
            String host = uri.getHost();
            if (host == null || !host.equalsIgnoreCase("image.pollinations.ai")) {
                throw ApiException.badRequest("Host image non autorisé");
            }
            HttpHeaders reqHeaders = new HttpHeaders();
            reqHeaders.set(HttpHeaders.ACCEPT, "image/*");
            reqHeaders.set(HttpHeaders.USER_AGENT, "Mozilla/5.0");
            ResponseEntity<byte[]> upstream = restTemplate.exchange(
                    uri,
                    org.springframework.http.HttpMethod.GET,
                    new org.springframework.http.HttpEntity<>(reqHeaders),
                    byte[].class
            );
            byte[] body = upstream.getBody();
            if (body == null || body.length == 0) throw ApiException.internalServerError("Image vide");
            MediaType contentType = upstream.getHeaders().getContentType();
            if (contentType == null || !contentType.toString().startsWith("image/")) {
                contentType = MediaType.IMAGE_JPEG;
            }
            return ResponseEntity.ok()
                    .header(HttpHeaders.CACHE_CONTROL, "no-cache, no-store, max-age=0")
                    .contentType(contentType)
                    .body(body);
        } catch (ApiException e) {
            throw e;
        } catch (Exception e) {
            throw ApiException.internalServerError("Impossible de charger l'image cover.");
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteGame(@PathVariable Long id) {
        educatorGameService.deleteGame(id);
        return ResponseEntity.noContent().build();
    }
}
