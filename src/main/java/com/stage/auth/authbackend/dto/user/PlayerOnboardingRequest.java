package com.stage.auth.authbackend.dto.user;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Body de la requête PATCH /api/users/me/onboarding.
 * Uniquement pour les joueurs. Pays et région requis (noms).
 * Le backend fait find-or-create dans pays/regions.
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PlayerOnboardingRequest {

    @JsonProperty("paysNom")
    private String paysNom;

    @JsonProperty("regionNom")
    private String regionNom;

    @JsonProperty("avatarUrl")
    private String avatarUrl;
}
