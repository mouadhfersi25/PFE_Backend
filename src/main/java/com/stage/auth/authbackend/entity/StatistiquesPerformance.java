package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "statistiques_performance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StatistiquesPerformance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "precision")
    private Double precisionScore;

    @Column(name = "taux_reussite")
    private Double tauxReussite;

    @Column(name = "temps_moyen_reponse")
    private Double tempsMoyenReponse;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_session", nullable = false)
    private SessionJeu sessionJeu;
}

