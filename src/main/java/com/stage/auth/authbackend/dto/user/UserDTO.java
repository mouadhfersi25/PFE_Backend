package com.stage.auth.authbackend.dto.user;

import com.stage.auth.authbackend.entity.EtatCompte;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * DTO en correspondance stricte à 100 % avec la table users.
 * Le champ password est toujours null en réponse API (sécurité).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class UserDTO {

    private Long id;

    private String nom;
    private String prenom;
    private String email;
    /** Ne jamais remplir en réponse API (toujours null). */
    private String password;

    private String telephone;
    private String avatarUrl;

    private String role;
    private EtatCompte etatCompte;
    private boolean enabled;

    private LocalDate dateDeNaissance;

    private Integer niveau;
    private Integer scoreTotal;
    private Integer pointsExperience;

    /** id_region (FK vers regions) */
    private Long idRegion;
    /** id_pays (dérivé de region si présent, pour affichage) */
    private Long idPays;
    private String paysNom;
    private String regionNom;

    /** Joueur a complété pays/région/ville */
    private boolean onboardingCompleted;
    /** id_genre (FK vers genres) */
    private Long idGenre;

    private String resetToken;
    private LocalDateTime resetTokenExpiry;
    private String tokenVerification;
    private LocalDateTime dateExpirationToken;

    private LocalDateTime dateDerniereConnexion;
    private LocalDateTime dateCreation;
}
