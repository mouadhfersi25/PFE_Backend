package com.britechnology.edugame.dto.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ForgotPasswordRequest {

    @NotBlank(message = "L'e-mail est requis")
    @Email(message = "Format d'e-mail invalide")
    private String email;
}
