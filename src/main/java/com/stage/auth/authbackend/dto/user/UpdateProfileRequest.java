package com.stage.auth.authbackend.dto.user;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class UpdateProfileRequest {

    private String nom;
    private String prenom;
    private String telephone;
    private String avatarUrl;
    private String email;
}
