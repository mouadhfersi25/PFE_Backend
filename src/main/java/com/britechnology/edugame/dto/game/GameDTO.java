package com.britechnology.edugame.dto.game;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.britechnology.edugame.entity.Difficulte;
import com.britechnology.edugame.entity.EtatJeu;
import com.britechnology.edugame.entity.ModeJeu;
import com.britechnology.edugame.entity.QuizVariant;
import com.britechnology.edugame.entity.TypeJeu;
import lombok.*;

import java.time.LocalDateTime;

/**
 * DTO pour les APIs Games (admin + éducateur).
 * Aligné sur l'entité Jeu (table jeux). Frontend: GameDTO (id, titre, description, difficulte, ageMin, ageMax, typeJeu, modeJeu, actif, dureeMinutes, dateCreation).
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

    private Difficulte difficulte;
    private Integer ageMin;
    private Integer ageMax;

    private TypeJeu typeJeu;
    private ModeJeu modeJeu;

    private QuizVariant quizVariant;

    private boolean actif;

    /** Vrai si l'éducateur a demandé la réactivation d'un jeu désactivé ; en attente de décision admin. */
    private boolean reactivationPending;

    private Integer dureeMinutes;

    private String coverImageUrl;

    /** Éducateur créateur du jeu (null pour les jeux créés directement par l'admin). */
    private Long educatorId;
    private String educatorName;
    private String educatorEmail;

    /** Nombre de parties jouées (toutes sessions confondues) pour ce jeu. */
    private long sessionsCount;

    private EtatJeu etat;
    private String latestRefusalReason;
    /** Motif de la dernière désactivation admin (signalement), s'il y en a eu une. */
    private String latestDeactivationReason;
    /** Motif du dernier refus admin d'une demande de réactivation, s'il y en a eu un. */
    private String latestReactivationRejectionReason;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime dateCreation;
}
