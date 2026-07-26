package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "game_review_history")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GameReviewHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "jeu_id", nullable = false)
    private Jeu jeu;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(name = "action", nullable = false, length = 20)
    private GameReviewAction action;

    @Column(name = "motif_refus", columnDefinition = "TEXT")
    private String motifRefus;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;
}
