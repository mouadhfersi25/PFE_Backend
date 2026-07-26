package com.stage.auth.authbackend.dto.game;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.stage.auth.authbackend.entity.EtatJeu;
import com.stage.auth.authbackend.entity.ModeJeu;
import com.stage.auth.authbackend.entity.TypeJeu;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO pour les APIs Games (admin + éducateur).
 * Aligné sur l'entité Jeu (table jeux). Frontend: GameDTO (id, titre, description, difficulte, ageMin, ageMax, typeJeu, modeJeu, actif, dureeMinutes, icone, dateCreation).
 */
@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class GameDTO {

    private Long id;

    private String titre;
    private String description;

    private Integer difficulte;
    private Integer ageMin;
    private Integer ageMax;

    private TypeJeu typeJeu;
    private ModeJeu modeJeu;

    private boolean actif;

    private Integer dureeMinutes;

    private String icone;
    private String coverImageUrl;

    private EtatJeu etat;
    private String latestRefusalReason;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateCreation;
}
