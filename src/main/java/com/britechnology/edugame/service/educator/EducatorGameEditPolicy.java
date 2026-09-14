package com.britechnology.edugame.service.educator;

import com.britechnology.edugame.entity.EtatJeu;
import com.britechnology.edugame.entity.Jeu;
import com.britechnology.edugame.exception.ApiException;

/**
 * Un jeu finalisé (soumis à l’admin ou traité) n’est plus modifiable par l’éducateur
 * (métadonnées et contenu). La suppression du jeu elle-même reste possible via {@code deleteGame}.
 * Les états {@link EtatJeu#BROUILLON} et {@link EtatJeu#REFUSE}
 * autorisent les écritures de modification.
 * <p>
 * Exception : un jeu {@link EtatJeu#ACCEPTE} mais désactivé par l'admin suite à un signalement
 * ({@code actif=false}) redevient modifiable, le temps que l'éducateur corrige le contenu avant
 * de redemander la réactivation. Dès que la demande de réactivation est envoyée
 * ({@code reactivationPending=true}), le jeu est de nouveau verrouillé en attente de la décision admin.
 */
public final class EducatorGameEditPolicy {

    private EducatorGameEditPolicy() {}

    public static void requireDraft(Jeu jeu) {
        boolean correctingDeactivatedGame = jeu.getEtat() == EtatJeu.ACCEPTE
                && !jeu.isActif()
                && !jeu.isReactivationPending();
        if (correctingDeactivatedGame) {
            return;
        }
        if (jeu.getEtat() != EtatJeu.BROUILLON && jeu.getEtat() != EtatJeu.REFUSE) {
            throw ApiException.badRequest("Ce jeu est en cours de validation ou déjà accepté : modification impossible.");
        }
    }
}
