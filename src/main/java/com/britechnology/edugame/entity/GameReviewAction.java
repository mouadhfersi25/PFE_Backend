package com.britechnology.edugame.entity;

public enum GameReviewAction {
    ACCEPTE,
    REFUSE,
    /** Jeu déjà accepté, désactivé par l'admin suite à un signalement joueur validé. */
    DESACTIVE,
    /** L'éducateur a corrigé un jeu désactivé et demande sa réactivation à l'admin. */
    REACTIVATION_DEMANDEE,
    /** L'admin a validé la demande de réactivation : le jeu redevient actif. */
    REACTIVATION_ACCEPTEE,
    /** L'admin a refusé la demande de réactivation : le jeu reste désactivé. */
    REACTIVATION_REFUSEE,
    /** L'éducateur annule lui-même sa demande de réactivation (avant décision admin) pour continuer à corriger. */
    REACTIVATION_ANNULEE
}
