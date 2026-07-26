package com.stage.auth.authbackend.service.educator;

import com.stage.auth.authbackend.dto.educator.CreateLogicPuzzleRequest;
import com.stage.auth.authbackend.dto.educator.LogicPuzzleDTO;
import com.stage.auth.authbackend.dto.educator.UpdateLogicPuzzleRequest;
import com.stage.auth.authbackend.entity.Jeu;
import com.stage.auth.authbackend.entity.PuzzleLogique;
import com.stage.auth.authbackend.entity.TypeJeu;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.game.JeuRepository;
import com.stage.auth.authbackend.repository.game.PuzzleLogiqueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class EducatorLogicService {

    private final PuzzleLogiqueRepository puzzleLogiqueRepository;
    private final JeuRepository jeuRepository;
    private static final Pattern TYPE_JSON_PATTERN = Pattern.compile("\"type\"\\s*:\\s*\"([^\"]+)\"");

    public List<LogicPuzzleDTO> listByGame(Long jeuId) {
        Jeu jeu = validateJeuType(jeuId, TypeJeu.LOGIQUE);
        return puzzleLogiqueRepository.findByJeuId(jeuId).stream()
                .map(p -> toDTO(p, jeu))
                .collect(Collectors.toList());
    }

    @Transactional
    public LogicPuzzleDTO create(CreateLogicPuzzleRequest request) {
        if (request == null || request.getJeuId() == null) {
            throw ApiException.badRequest("jeuId est requis");
        }
        Jeu jeu = validateJeuType(request.getJeuId(), TypeJeu.LOGIQUE);
        EducatorGameEditPolicy.requireDraft(jeu);
        PuzzleLogique puzzle = PuzzleLogique.builder()
                .jeu(jeu)
                .enonce(request.getEnonce())
                .sousType(normalizeSousType(request.getSousType(), request.getDonnees()))
                .donnees(request.getDonnees())
                .bonneReponse(request.getBonneReponse())
                .indice(request.getIndice())
                .difficulte(request.getDifficulte())
                .build();
        puzzle = puzzleLogiqueRepository.save(puzzle);
        touchGameContent(jeu);
        return toDTO(puzzle, jeu);
    }

    @Transactional
    public LogicPuzzleDTO update(Long id, UpdateLogicPuzzleRequest request) {
        PuzzleLogique puzzle = puzzleLogiqueRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Puzzle introuvable"));
        if (puzzle.getJeu() == null || puzzle.getJeu().getTypeJeu() != TypeJeu.LOGIQUE) {
            throw ApiException.badRequest("Ce puzzle n'est pas lié à un jeu de type LOGIQUE");
        }
        EducatorGameEditPolicy.requireDraft(puzzle.getJeu());
        if (request.getEnonce() != null) puzzle.setEnonce(request.getEnonce());
        if (request.getSousType() != null || request.getDonnees() != null) {
            puzzle.setSousType(normalizeSousType(request.getSousType(), request.getDonnees() != null ? request.getDonnees() : puzzle.getDonnees()));
        }
        if (request.getDonnees() != null) puzzle.setDonnees(request.getDonnees());
        if (request.getBonneReponse() != null) puzzle.setBonneReponse(request.getBonneReponse());
        if (request.getIndice() != null) puzzle.setIndice(request.getIndice());
        if (request.getDifficulte() != null) puzzle.setDifficulte(request.getDifficulte());
        puzzle = puzzleLogiqueRepository.save(puzzle);
        touchGameContent(puzzle.getJeu());
        return toDTO(puzzle, puzzle.getJeu());
    }

    @Transactional
    public void delete(Long id) {
        PuzzleLogique puzzle = puzzleLogiqueRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Puzzle introuvable"));
        if (puzzle.getJeu() == null || puzzle.getJeu().getTypeJeu() != TypeJeu.LOGIQUE) {
            throw ApiException.badRequest("Ce puzzle n'est pas lié à un jeu de type LOGIQUE");
        }
        EducatorGameEditPolicy.requireDraft(puzzle.getJeu());
        touchGameContent(puzzle.getJeu());
        puzzleLogiqueRepository.delete(puzzle);
    }

    private void touchGameContent(Jeu jeu) {
        jeu.setLastContentUpdateAt(LocalDateTime.now());
        jeuRepository.save(jeu);
    }

    private Jeu validateJeuType(Long jeuId, TypeJeu expected) {
        Jeu jeu = jeuRepository.findById(jeuId)
                .orElseThrow(() -> ApiException.notFound("Jeu introuvable"));
        if (jeu.getTypeJeu() != expected) {
            throw ApiException.badRequest("Le jeu n'est pas de type " + expected.name());
        }
        return jeu;
    }

    private static LogicPuzzleDTO toDTO(PuzzleLogique p, Jeu jeu) {
        return LogicPuzzleDTO.builder()
                .id(p.getId())
                .jeuId(jeu.getId())
                .jeuTitre(jeu.getTitre())
                .enonce(p.getEnonce())
                .sousType(normalizeSousType(p.getSousType(), p.getDonnees()))
                .donnees(p.getDonnees())
                .bonneReponse(p.getBonneReponse())
                .indice(p.getIndice())
                .difficulte(p.getDifficulte())
                .build();
    }

    private static String normalizeSousType(String rawSousType, String rawDonnees) {
        String candidate = (rawSousType != null && !rawSousType.isBlank()) ? rawSousType : extractTypeFromDonnees(rawDonnees);
        if (candidate == null || candidate.isBlank()) return "DEDUCTION";
        String normalized = candidate.trim().toUpperCase();
        return switch (normalized) {
            case "SUITE_LOGIQUE", "INTRUS", "DEDUCTION", "COLOR_MATCH" -> normalized;
            default -> "DEDUCTION";
        };
    }

    private static String extractTypeFromDonnees(String rawDonnees) {
        if (rawDonnees == null || rawDonnees.isBlank()) return null;
        Matcher matcher = TYPE_JSON_PATTERN.matcher(rawDonnees);
        return matcher.find() ? matcher.group(1) : null;
    }
}
