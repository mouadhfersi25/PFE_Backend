package com.britechnology.edugame.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "parametres_reflexe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParametresReflexe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Jeu de type REFLEXE auquel ces paramètres appartiennent.
     */
    @OneToOne(optional = false)
    @JoinColumn(name = "id_jeu", nullable = false, unique = true)
    private Jeu jeu;

    /**
     * Nombre de rounds / essais dans la partie.
     */
    @Column(name = "nombre_rounds", nullable = false)
    private Integer nombreRounds;

    /**
     * Temps de réaction maximum en millisecondes (optionnel).
     */
    @Column(name = "temps_reaction_max_ms")
    private Integer tempsReactionMaxMs;

    /**
     * Type de stimuli utilisés : COULEURS, FORMES, SONS, etc.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_stimuli", length = 50)
    private ReflexStimulusType typeStimuli;

    /**
     * Modèle de gameplay réflexe : CLASSIC, GO_NO_GO, CHOICE_REACTION.
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "modele_reflexe", length = 50)
    private ReflexModel modeleReflexe;

    /**
     * Ratio de pièges (0-100) utilisé pour GO_NO_GO.
     */
    @Column(name = "no_go_ratio")
    private Integer noGoRatio;

    /**
     * Nombre de choix à afficher par round pour CHOICE_REACTION.
     */
    @Column(name = "choice_target_count")
    private Integer choiceTargetCount;

    /**
     * Niveau de difficulté spécifique (optionnel).
     */
    private Integer difficulte;
}

