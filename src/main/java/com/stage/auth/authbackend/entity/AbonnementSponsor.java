package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "abonnements_sponsor")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AbonnementSponsor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_debut")
    private LocalDate dateDebut;

    @Column(name = "date_fin")
    private LocalDate dateFin;

    @Column(name = "montant")
    private Double montant;

    @Column(name = "nb_bannieres_louees")
    private Integer nbBannieresLouees;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_sponsor", nullable = false)
    private Sponsor sponsor;
}

