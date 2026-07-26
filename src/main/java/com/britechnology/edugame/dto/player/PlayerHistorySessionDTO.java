package com.britechnology.edugame.dto.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerHistorySessionDTO {
    private Long id;
    private Long gameId;
    private String gameTitle;
    private String gameType;
    private LocalDateTime dateDebut;
    private LocalDateTime dateFin;
    private Integer durationSeconds;
    private Integer scoreFinal;
    private Integer niveauAtteint;
    private Boolean reussite;
    private String statut;
    private String mode;
    private Integer accuracy;
    private Integer reactionTime;
}
