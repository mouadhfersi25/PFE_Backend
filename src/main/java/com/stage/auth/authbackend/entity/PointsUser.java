package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "points_user")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PointsUser {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "total_points")
    private Integer totalPoints;

    @Column(name = "xp_total")
    private Integer xpTotal;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private User utilisateur;
}

