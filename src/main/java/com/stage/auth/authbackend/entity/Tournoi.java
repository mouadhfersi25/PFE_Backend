package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "tournois")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Tournoi {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "max_participants")
    private Integer maxParticipants;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_tournoi", nullable = false)
    private EtatTournoi etatTournoi;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_jeu", nullable = false)
    private Jeu jeu;
}

