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

    @NotBlank(message = "L'email est requis")
    @Email(message = "Format d'email invalide")
    private String email;

    @NotBlank(message = "Le mot de passe est requis")
    @Size(min = 6, message = "Le mot de passe doit contenir au moins 6 caractères")
    private String password;

    @NotNull(message = "La date de naissance est requise")
    @Past(message = "La date de naissance doit être dans le passé")
    private LocalDate dateDeNaissance;

    // Optionnel : 8 chiffres (Tunisie)
    @Pattern(regexp = "^[0-9]{8}$", message = "Téléphone invalide (8 chiffres)")
    private String telephone;
}
