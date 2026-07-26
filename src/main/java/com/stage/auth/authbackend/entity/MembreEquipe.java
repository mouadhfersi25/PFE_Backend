package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "membres_equipe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MembreEquipe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 100)
    private String role;

    @Column(name = "date_entree")
    private LocalDate dateEntree;

    @Column(name = "date_sortie")
    private LocalDate dateSortie;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private User utilisateur;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_equipe", nullable = false)
    private Equipe equipe;
}

