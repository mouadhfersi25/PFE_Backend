package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "badges_utilisateur")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BadgeUtilisateur {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_badge", nullable = false)
    private Badge badge;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private User utilisateur;

    @Column(name = "date_obtention")
    private LocalDate dateObtention;
}

