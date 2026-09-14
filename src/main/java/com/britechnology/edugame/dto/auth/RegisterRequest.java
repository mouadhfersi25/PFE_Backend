package com.britechnology.edugame.dto.auth;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

@Data
public class RegisterRequest {

    @NotBlank(message = "Le nom est requis")
    @Size(min = 2, max = 50)
    private String nom;

    @NotBlank(message = "Le prénom est requis")
    @Size(min = 2, max = 50)
    private String prenom;

    @NotBlank(message = "L'e-mail est requis")
    @Email(message = "Format d'e-mail invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est requis")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    @NotNull(message = "La date de naissance est requise")
    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDate dateDeNaissance;

    /** CIN (8 chiffres) — requis pour l'inscription parent. */
    @NotBlank(message = "Le CIN est requis")
    @Pattern(regexp = "^[0-9]{8}$", message = "CIN invalide (8 chiffres)")
    private String cin;

    /**
     * Téléphone — requis pour l'inscription parent. Le frontend préfixe automatiquement
     * l'indicatif du pays sélectionné (ex. Tunisie -> +216XXXXXXXX) ; on accepte donc soit
     * ce format international, soit 8 chiffres bruts en repli.
     */
    @NotBlank(message = "Le téléphone est requis")
    @Pattern(regexp = "^(\\+[0-9]{1,4})?[0-9]{6,14}$", message = "Téléphone invalide")
    private String telephone;

    /**
     * Pays choisi via le sélecteur "drapeau" du champ téléphone (nom anglais, ex. "Tunisia").
     * Sert aussi de valeur par défaut pour l'onboarding des joueurs (enfants) rattachés à ce parent.
     */
    @NotBlank(message = "Le pays est requis")
    private String paysNom;

    /** Genre déclaré (choix radio) — HOMME ou FEMME. */
    @NotBlank(message = "Le genre est requis")
    @Pattern(regexp = "^(HOMME|FEMME)$", message = "Genre invalide")
    private String genre;
}
