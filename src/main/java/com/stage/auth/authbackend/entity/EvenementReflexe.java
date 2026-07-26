package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "evenements_reflexe")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvenementReflexe {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_evenement", length = 150)
    private String typeEvenement;

    @Column(name = "temps_limit")
    private Double tempsLimit;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_jeu", nullable = false)
    private Jeu jeu;
}

