package com.britechnology.edugame.service.educator;

import com.britechnology.edugame.dto.educator.CreateMemoryCardRequest;
import com.britechnology.edugame.dto.educator.MemoryCardDTO;
import com.britechnology.edugame.dto.educator.UpdateMemoryCardRequest;
import com.britechnology.edugame.entity.CarteMemoire;
import com.britechnology.edugame.entity.Jeu;
import com.britechnology.edugame.entity.TypeJeu;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.game.CarteMemoireRepository;
import com.britechnology.edugame.repository.game.JeuRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class EducatorMemoryService {

    private final CarteMemoireRepository carteMemoireRepository;
    private final JeuRepository jeuRepository;

    public List<MemoryCardDTO> listByGame(Long jeuId) {
        Jeu jeu = validateJeuType(jeuId, TypeJeu.MEMOIRE);
        return carteMemoireRepository.findByJeuId(jeuId).stream()
                .map(c -> toDTO(c, jeu))
                .collect(Collectors.toList());
    }

    @Transactional
    public MemoryCardDTO create(CreateMemoryCardRequest request) {
        if (request == null || request.getJeuId() == null) {
            throw ApiException.badRequest("jeuId est requis");
        }
        Jeu jeu = validateJeuType(request.getJeuId(), TypeJeu.MEMOIRE);
        EducatorGameEditPolicy.requireDraft(jeu);
        CarteMemoire carte = CarteMemoire.builder()
                .jeu(jeu)
                .symbole(clampSymboleOrThrow(request.getSymbole()))
                .cardType(normalizeCardType(request.getCardType()))
                .cardValue(trimToNull(request.getCardValue()))
                .sousType(normalizeMemorySousType(request.getSousType()))
                .pairKey(request.getPairKey())
                .categorie(request.getCategorie())
                .build();
        carte = carteMemoireRepository.save(carte);
        touchGameContent(jeu);
        return toDTO(carte, jeu);
    }

    @Transactional
    public MemoryCardDTO update(Long id, UpdateMemoryCardRequest request) {
        CarteMemoire carte = carteMemoireRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Carte introuvable"));
        if (carte.getJeu() == null || carte.getJeu().getTypeJeu() != TypeJeu.MEMOIRE) {
            throw ApiException.badRequest("Cette carte n'est pas liée à un jeu de type MEMOIRE");
        }
        EducatorGameEditPolicy.requireDraft(carte.getJeu());
        if (request.getSymbole() != null) carte.setSymbole(clampSymboleOrThrow(request.getSymbole()));
        if (request.getCardType() != null) carte.setCardType(normalizeCardType(request.getCardType()));
        if (request.getCardValue() != null) carte.setCardValue(trimToNull(request.getCardValue()));
        if (request.getSousType() != null) carte.setSousType(normalizeMemorySousType(request.getSousType()));
        if (request.getPairKey() != null) carte.setPairKey(request.getPairKey());
        if (request.getCategorie() != null) carte.setCategorie(request.getCategorie());
        carte = carteMemoireRepository.save(carte);
        touchGameContent(carte.getJeu());
        return toDTO(carte, carte.getJeu());
    }

    @Transactional
    public void delete(Long id) {
        CarteMemoire carte = carteMemoireRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Carte introuvable"));
        if (carte.getJeu() == null || carte.getJeu().getTypeJeu() != TypeJeu.MEMOIRE) {
            throw ApiException.badRequest("Cette carte n'est pas liée à un jeu de type MEMOIRE");
        }
        EducatorGameEditPolicy.requireDraft(carte.getJeu());
        touchGameContent(carte.getJeu());
        carteMemoireRepository.delete(carte);
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

    private static MemoryCardDTO toDTO(CarteMemoire c, Jeu jeu) {
        return MemoryCardDTO.builder()
                .id(c.getId())
                .jeuId(jeu.getId())
                .jeuTitre(jeu.getTitre())
                .symbole(c.getSymbole())
                .cardType(normalizeCardType(c.getCardType()))
                .cardValue(c.getCardValue())
                .sousType(normalizeMemorySousType(c.getSousType()))
                .pairKey(c.getPairKey())
                .categorie(c.getCategorie())
                .build();
    }

    private static String normalizeCardType(String raw) {
        String value = trimToNull(raw);
        if (value == null) return "EMOJI";
        return switch (value.toUpperCase()) {
            case "EMOJI", "TEXT", "IMAGE", "COLOR" -> value.toUpperCase();
            default -> "EMOJI";
        };
    }

    private static String normalizeMemorySousType(String raw) {
        String value = trimToNull(raw);
        if (value == null) return "DEFAULT";
        return switch (value.toUpperCase()) {
            case "IMAGE_WORD_PAIR", "COLOR_WORD_PAIR", "BILINGUAL_WORD_PAIR", "DEFAULT" -> value.toUpperCase();
            default -> "DEFAULT";
        };
    }

    private static String trimToNull(String value) {
        if (value == null) return null;
        String normalized = value.trim();
        return normalized.isEmpty() ? null : normalized;
    }

    /**
     * Colonne {@code symbole} limitée à 100 caractères (JPA / PostgreSQL). Une URL d'image doit aller dans {@code cardValue}.
     */
    private static String clampSymboleOrThrow(String symbole) {
        if (symbole == null || symbole.isBlank()) {
            throw ApiException.badRequest("symbole est requis");
        }
        String t = symbole.trim();
        if (t.length() > 100) {
            throw ApiException.badRequest(
                    "Le champ symbole (icône) ne peut pas dépasser 100 caractères. Mettez l'URL de l'image uniquement dans la valeur de la carte de type Image.");
        }
        return t;
    }
}
