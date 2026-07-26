package com.stage.auth.authbackend.service.educator;

import com.stage.auth.authbackend.dto.educator.CreateMemoryCardRequest;
import com.stage.auth.authbackend.dto.educator.MemoryCardDTO;
import com.stage.auth.authbackend.dto.educator.UpdateMemoryCardRequest;
import com.stage.auth.authbackend.entity.CarteMemoire;
import com.stage.auth.authbackend.entity.Jeu;
import com.stage.auth.authbackend.entity.TypeJeu;
import com.stage.auth.authbackend.exception.ApiException;
import com.stage.auth.authbackend.repository.game.CarteMemoireRepository;
import com.stage.auth.authbackend.repository.game.JeuRepository;
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
                .symbole(request.getSymbole())
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
        if (request.getSymbole() != null) carte.setSymbole(request.getSymbole());
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
}
