package com.britechnology.edugame.entity;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

/**
 * Convertit la colonne "genre" (users.genre) en {@link Genre}, en tolérant les valeurs
 * vides/invalides déjà présentes en base (ex. comptes créés avant que le genre soit rendu
 * obligatoire) : sans ce convertisseur, Hibernate lève une exception dès qu'une seule ligne a
 * un genre invalide, ce qui casse TOUTE requête chargeant la table users (ex. liste admin).
 */
@Converter(autoApply = false)
public class GenreConverter implements AttributeConverter<Genre, String> {

    @Override
    public String convertToDatabaseColumn(Genre attribute) {
        return attribute == null ? null : attribute.name();
    }

    @Override
    public Genre convertToEntityAttribute(String dbData) {
        if (dbData == null || dbData.isBlank()) {
            return null;
        }
        try {
            return Genre.valueOf(dbData.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
