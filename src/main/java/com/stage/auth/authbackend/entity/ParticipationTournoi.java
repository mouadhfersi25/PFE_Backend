package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "participations_tournoi")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ParticipationTournoi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "score_final")
    private Integer scoreFinal;

    @Column(name = "classement")
    private Integer classement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private User utilisateur;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_equipe", nullable = false)
    private Equipe equipe;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_tournoi", nullable = false)
    private Tournoi tournoi;
}

