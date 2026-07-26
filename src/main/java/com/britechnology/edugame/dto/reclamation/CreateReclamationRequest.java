package com.britechnology.edugame.dto.reclamation;

import com.britechnology.edugame.entity.MotifReclamation;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CreateReclamationRequest {

    @NotNull
    private Long sessionId;

    @NotNull
    private Long gameId;

    @NotNull
    private MotifReclamation motif;

    private String commentaire;
}
