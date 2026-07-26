package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "profils_educatifs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProfilEducatif {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "niveau_scolaire", length = 150)
    private String niveauScolaire;

    @Column(name = "style_apprentissage", length = 150)
    private String styleApprentissage;

    @Column(name = "objectif_apprentissage", length = 255)
    private String objectifApprentissage;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private User utilisateur;
}

