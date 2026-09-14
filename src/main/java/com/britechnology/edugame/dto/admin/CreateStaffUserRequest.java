package com.britechnology.edugame.dto.admin;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;

/** Création d'un compte EDUCATEUR ou SPONSOR par l'administrateur. */
@Data
public class CreateStaffUserRequest {

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

    /** EDUCATEUR ou SPONSOR */
    @NotBlank(message = "Le rôle est requis")
    private String role;

    @NotBlank(message = "Le téléphone est requis")
    @Pattern(regexp = "^[0-9]{8}$", message = "Téléphone invalide (8 chiffres)")
    private String telephone;

    /** Genre déclaré (choix radio) — HOMME ou FEMME. */
    @NotBlank(message = "Le genre est requis")
    @Pattern(regexp = "^(HOMME|FEMME)$", message = "Genre invalide")
    private String genre;
}
