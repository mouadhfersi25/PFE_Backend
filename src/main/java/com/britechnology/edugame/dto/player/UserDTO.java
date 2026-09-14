package com.britechnology.edugame.dto.player;

import com.britechnology.edugame.entity.EtatCompte;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

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
    private String cin;
    private String avatarUrl;

    private String role;
    private EtatCompte etatCompte;
    private boolean enabled;

    private LocalDate dateDeNaissance;

    private Integer niveau;
    private Integer scoreTotal;
    private Integer pointsExperience;
    private Integer xpToNextLevel;
    private Integer currentStreakDays;
    private Integer bestStreakDays;
    private LocalDate lastStreakDate;

    /** id_region (FK vers regions) */
    private Long idRegion;
    /** id_pays (dérivé de region si présent, pour affichage) */
    private Long idPays;
    private String paysNom;
    private String regionNom;

    /** Joueur a complété pays/région/ville */
    private boolean onboardingCompleted;
    /** Genre déclaré à l'inscription (HOMME / FEMME). */
    private String genre;

    private String resetToken;
    private LocalDateTime resetTokenExpiry;
    private String tokenVerification;
    private LocalDateTime dateExpirationToken;

    private LocalDateTime dateDerniereConnexion;
    private LocalDateTime dateCreation;

    /** Id du compte PARENT si cet utilisateur est un JOUEUR rattaché. */
    private Long idParent;

    /**
     * Pays choisi par le compte PARENT à son inscription, si ce joueur lui est rattaché.
     * Utilisé côté frontend comme valeur par défaut lors de l'onboarding du joueur
     * (pré-sélection du pays, modifiable par l'utilisateur).
     */
    private String parentPaysNom;
}
