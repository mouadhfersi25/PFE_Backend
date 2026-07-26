package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "sponsors")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Sponsor {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nom_entreprise", nullable = false, length = 200)
    private String nomEntreprise;

    @Column(name = "budget_total")
    private Double budgetTotal;

    @Column(name = "date_debut_partenariat")
    private LocalDate dateDebutPartenariat;

    @Column(name = "date_fin_partenariat")
    private LocalDate dateFinPartenariat;

    @Column(name = "statut_partenariat", length = 100)
    private String statutPartenariat;
}

