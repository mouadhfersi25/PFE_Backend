package com.britechnology.edugame.entity;

/** Difficulté d'un jeu (table jeux), choisie par l'éducateur/l'admin — plus de saisie numérique. */
public enum Difficulte {
    FACILE(2),
    MOYEN(5),
    DIFFICILE(8);

    /**
     * Poids numérique représentatif (ancienne échelle 0-10), utilisé uniquement là où un nombre
     * est nécessaire : multiplicateur de score et prompts IA. Ne pilote plus la persistance.
     */
    private final int weight;

    Difficulte(int weight) {
        this.weight = weight;
    }

    public int getWeight() {
        return weight;
    }
}
