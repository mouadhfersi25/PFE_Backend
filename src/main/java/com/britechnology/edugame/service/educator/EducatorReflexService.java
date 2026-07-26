package com.britechnology.edugame.service.educator;

import com.britechnology.edugame.dto.educator.CreateOrUpdateReflexSettingsRequest;
import com.britechnology.edugame.dto.educator.ReflexSettingsDTO;
import com.britechnology.edugame.entity.Jeu;
import com.britechnology.edugame.entity.ParametresReflexe;
import com.britechnology.edugame.entity.ReflexModel;
import com.britechnology.edugame.entity.ReflexStimulusType;
import com.britechnology.edugame.entity.TypeJeu;
import com.britechnology.edugame.exception.ApiException;
import com.britechnology.edugame.repository.game.JeuRepository;
import com.britechnology.edugame.repository.game.ParametresReflexeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class EducatorReflexService {

    private final ParametresReflexeRepository parametresReflexeRepository;
    private final JeuRepository jeuRepository;

    public ReflexSettingsDTO getByGame(Long jeuId) {
        Jeu jeu = validateJeuType(jeuId, TypeJeu.REFLEXE);
        return parametresReflexeRepository.findByJeuId(jeuId)
                .map(p -> toDTO(p, jeu))
                .orElse(null);
    }

    @Transactional
    public ReflexSettingsDTO createOrUpdate(CreateOrUpdateReflexSettingsRequest request) {
        if (request == null || request.getJeuId() == null) {
            throw ApiException.badRequest("jeuId est requis");
        }
        if (request.getNombreRounds() == null) {
            throw ApiException.badRequest("nombreRounds est requis");
        }
        ReflexModel modeleReflexe = request.getModeleReflexe() != null ? request.getModeleReflexe() : ReflexModel.CLASSIC;
        ReflexStimulusType typeStimuli = request.getTypeStimuli() != null ? request.getTypeStimuli() : ReflexStimulusType.TARGET_ICON;
        Integer noGoRatio = resolveNoGoRatio(modeleReflexe, request.getNoGoRatio());
        Integer choiceTargetCount = resolveChoiceTargetCount(modeleReflexe, request.getChoiceTargetCount());
        Jeu jeu = validateJeuType(request.getJeuId(), TypeJeu.REFLEXE);
        EducatorGameEditPolicy.requireDraft(jeu);
        ParametresReflexe params = parametresReflexeRepository.findByJeuId(jeu.getId()).orElse(null);
        if (params == null) {
            params = ParametresReflexe.builder()
                    .jeu(jeu)
                    .nombreRounds(request.getNombreRounds())
                    .tempsReactionMaxMs(request.getTempsReactionMaxMs())
                    .typeStimuli(typeStimuli)
                    .modeleReflexe(modeleReflexe)
                    .noGoRatio(noGoRatio)
                    .choiceTargetCount(choiceTargetCount)
                    .difficulte(request.getDifficulte())
                    .build();
        } else {
            params.setNombreRounds(request.getNombreRounds());
            if (request.getTempsReactionMaxMs() != null) params.setTempsReactionMaxMs(request.getTempsReactionMaxMs());
            params.setTypeStimuli(typeStimuli);
            params.setModeleReflexe(modeleReflexe);
            params.setNoGoRatio(noGoRatio);
            params.setChoiceTargetCount(choiceTargetCount);
            if (request.getDifficulte() != null) params.setDifficulte(request.getDifficulte());
        }
        params = parametresReflexeRepository.save(params);
        touchGameContent(jeu);
        return toDTO(params, jeu);
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

    private static ReflexSettingsDTO toDTO(ParametresReflexe p, Jeu jeu) {
        return ReflexSettingsDTO.builder()
                .id(p.getId())
                .jeuId(jeu.getId())
                .jeuTitre(jeu.getTitre())
                .nombreRounds(p.getNombreRounds())
                .tempsReactionMaxMs(p.getTempsReactionMaxMs())
                .typeStimuli(p.getTypeStimuli() != null ? p.getTypeStimuli() : ReflexStimulusType.TARGET_ICON)
                .modeleReflexe(p.getModeleReflexe() != null ? p.getModeleReflexe() : ReflexModel.CLASSIC)
                .noGoRatio(p.getNoGoRatio())
                .choiceTargetCount(p.getChoiceTargetCount())
                .difficulte(p.getDifficulte())
                .build();
    }

    private static Integer resolveNoGoRatio(ReflexModel model, Integer requested) {
        if (model != ReflexModel.GO_NO_GO) {
            return null;
        }
        if (requested == null) {
            throw ApiException.badRequest("noGoRatio est requis pour le modèle GO_NO_GO");
        }
        return Math.max(10, Math.min(90, requested));
    }

    private static Integer resolveChoiceTargetCount(ReflexModel model, Integer requested) {
        if (model != ReflexModel.CHOICE_REACTION) {
            return null;
        }
        if (requested == null) {
            throw ApiException.badRequest("choiceTargetCount est requis pour le modèle CHOICE_REACTION");
        }
        return Math.max(2, Math.min(6, requested));
    }
}
