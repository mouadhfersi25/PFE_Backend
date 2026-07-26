package com.britechnology.edugame.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "niveaux")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Niveau {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "niveau", unique = true)
    private Integer niveau;

    @Column(name = "point_min")
    private Integer pointMin;

    @ManyToOne
    @JoinColumn(name = "badge_id")
    private Badge badge;
}

