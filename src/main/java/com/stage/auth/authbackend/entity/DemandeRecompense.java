package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "demandes_recompense")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class DemandeRecompense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 50)
    private String statut;

    @Column(name = "date_demande")
    private LocalDate dateDemande;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private User utilisateur;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_recompense", nullable = false)
    private Recompense recompense;

    @PrePersist
    protected void onCreate() {
        if (this.dateDemande == null) {
            this.dateDemande = LocalDate.now();
        }
        if (this.statut == null || this.statut.isBlank()) {
            this.statut = "PENDING";
        }
    }
}

