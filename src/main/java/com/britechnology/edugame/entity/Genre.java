package com.britechnology.edugame.entity;

import com.britechnology.edugame.exception.ApiException;

/** Genre déclaré à l'inscription (choix radio, obligatoire pour tous les comptes). */
public enum Genre {
    HOMME,
    FEMME;

    /** Parse une valeur de formulaire ("HOMME"/"FEMME", insensible à la casse) ou lève une 400. */
    public static Genre parse(String raw) {
        if (raw == null || raw.isBlank()) {
            throw ApiException.badRequest("Le genre est requis");
        }
        try {
            return Genre.valueOf(raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw ApiException.badRequest("Genre invalide. Valeurs : HOMME, FEMME");
        }
    }
}
