package com.stage.auth.authbackend.entity;

/**
 * Types de conditions prédéfinies pour débloquer un badge.
 * L'admin choisit dans cette liste (pas de saisie libre).
 */
public enum TypeConditionBadge {

    /** Score total minimum atteint (paramètre : score_condition) */
    SCORE_MIN,

    /** Première victoire (aucun paramètre) */
    FIRST_WIN,

    /** Avoir joué un nombre de parties (paramètre : score_condition = nombre) */
    GAMES_PLAYED,

    /** Série de connexion en jours (paramètre : score_condition = nombre de jours) */
    STREAK_DAYS,

    /** Gagner une partie de quiz */
    QUIZ_WIN,

    /** Compléter une partie sans erreur */
    PERFECT_GAME
}
