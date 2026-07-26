package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "localisations")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Localisation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Double latitude;

    @Column(nullable = false)
    private Double longitude;

    @Column(name = "date_enregistrement")
    private LocalDateTime dateEnregistrement;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private User utilisateur;
}

