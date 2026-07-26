package com.stage.auth.authbackend.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class UpdateRoleRequest {
    @NotBlank(message = "Role is required")
    private String role;
}
