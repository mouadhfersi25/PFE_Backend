package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "recommandations_ia")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RecommandationIA {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_jeu", length = 50)
    private String typeJeu;

    @Column(name = "score_confiance")
    private Double scoreConfiance;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private User utilisateur;
}

