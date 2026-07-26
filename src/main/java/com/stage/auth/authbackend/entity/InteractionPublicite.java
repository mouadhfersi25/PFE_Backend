package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "interactions_publicite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionPublicite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "type_interaction", length = 100)
    private String typeInteraction;

    @Column(name = "date_interaction")
    private LocalDateTime dateInteraction;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_publicite", nullable = false)
    private Publicite publicite;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private User utilisateur;
}

