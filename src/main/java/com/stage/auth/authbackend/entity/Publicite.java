package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "publicites")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Publicite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 255)
    private String contenu;

    @Column(name = "type_publicite", length = 100)
    private String typePublicite;

    @Column(name = "budget_utilise")
    private Double budgetUtilise;

    @Column(name = "nb_vues")
    private Integer nbVues;

    @Column(name = "nb_clics")
    private Integer nbClics;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_sponsor", nullable = false)
    private Sponsor sponsor;
}

