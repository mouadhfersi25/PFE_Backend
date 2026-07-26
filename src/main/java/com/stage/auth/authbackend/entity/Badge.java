package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "badges")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Badge {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(length = 255)
    private String description;

    /** Type de condition (liste prédéfinie). Null = ancien badge, traité comme SCORE_MIN. */
    @Enumerated(EnumType.STRING)
    @Column(name = "type_condition", length = 50)
    private TypeConditionBadge typeCondition;

    /** Valeur associée (ex. score min pour SCORE_MIN, nombre de parties pour GAMES_PLAYED). */
    @Column(name = "score_condition")
    private Integer scoreCondition;

    @Column(length = 255)
    private String icone;
}

