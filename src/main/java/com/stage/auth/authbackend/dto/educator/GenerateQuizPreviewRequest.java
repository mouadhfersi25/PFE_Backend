package com.stage.auth.authbackend.dto.educator;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class GenerateQuizPreviewRequest {

    @NotNull(message = "gameId est requis")
    private Long gameId;

    @Min(value = 1, message = "Le nombre de questions doit être au minimum 1")
    @Max(value = 10, message = "Le nombre de questions doit être au maximum 10")
    private Integer count = 5;
}
