package com.britechnology.edugame.dto.player;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateRoleRequest {
    @NotBlank(message = "Le rôle est requis")
    private String role;
}
