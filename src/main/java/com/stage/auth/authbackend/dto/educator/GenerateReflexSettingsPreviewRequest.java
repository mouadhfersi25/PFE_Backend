package com.stage.auth.authbackend.dto.educator;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateReflexSettingsPreviewRequest {
    @NotNull(message = "gameId est requis")
    private Long gameId;
}
