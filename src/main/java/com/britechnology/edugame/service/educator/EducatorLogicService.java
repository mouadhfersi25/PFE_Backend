package com.britechnology.edugame.service.educator;

import com.britechnology.edugame.dto.educator.CreateLogicPuzzleRequest;
import com.britechnology.edugame.dto.educator.LogicPuzzleDTO;
import com.britechnology.edugame.dto.educator.UpdateLogicPuzzleRequest;
import com.britechnology.edugame.entity.Jeu;
import com.britechnology.edugame.entity.PuzzleLogique;
import com.britechnology.edugame.entity.TypeJeu;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.game.JeuRepository;
import com.britechnology.edugame.repository.game.PuzzleLogiqueRepository;
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
        String sousType = normalizeSousType(request.getSousType(), request.getDonnees());
        LogicPuzzleContentValidator.validate(
                sousType,
                request.getEnonce(),
                request.getBonneReponse(),
                request.getDonnees()
        );
        PuzzleLogique puzzle = PuzzleLogique.builder()
                .jeu(jeu)
                .enonce(request.getEnonce())
                .sousType(sousType)
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
        String enonce = request.getEnonce() != null ? request.getEnonce() : puzzle.getEnonce();
        String donnees = request.getDonnees() != null ? request.getDonnees() : puzzle.getDonnees();
        String bonneReponse = request.getBonneReponse() != null ? request.getBonneReponse() : puzzle.getBonneReponse();
        String sousType = normalizeSousType(
                request.getSousType() != null ? request.getSousType() : puzzle.getSousType(),
                donnees
        );
        LogicPuzzleContentValidator.validate(sousType, enonce, bonneReponse, donnees);

        puzzle.setEnonce(enonce);
        puzzle.setSousType(sousType);
        puzzle.setDonnees(donnees);
        puzzle.setBonneReponse(bonneReponse);
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
