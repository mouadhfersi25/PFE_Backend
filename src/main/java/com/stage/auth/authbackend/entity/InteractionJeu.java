package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "interactions_jeu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionJeu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_interaction", length = 150)
    private String typeInteraction;

    @Column(name = "temps_reponse")
    private Double tempsReponse;

    @Column(name = "reponse_correcte")
    private Boolean reponseCorrecte;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_session", nullable = false)
    private SessionJeu sessionJeu;
}

