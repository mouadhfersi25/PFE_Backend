package com.britechnology.edugame.service.educator;

import com.britechnology.edugame.entity.EtatJeu;
import com.britechnology.edugame.entity.Jeu;
import com.britechnology.edugame.exception.ApiException;

/**
 * Un jeu finalisé (soumis à l’admin ou traité) n’est plus modifiable par l’éducateur
 * (métadonnées et contenu). La suppression du jeu elle-même reste possible via {@code deleteGame}.
 * Les états {@link EtatJeu#BROUILLON} et {@link EtatJeu#REFUSE}
 * autorisent les écritures de modification.
 */
public final class EducatorGameEditPolicy {

    private EducatorGameEditPolicy() {}

    public static void requireDraft(Jeu jeu) {
        if (jeu.getEtat() != EtatJeu.BROUILLON && jeu.getEtat() != EtatJeu.REFUSE) {
            throw ApiException.badRequest("Ce jeu est en cours de validation ou déjà accepté : modification impossible.");
        }
    }
}
