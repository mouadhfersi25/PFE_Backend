package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions_jeu")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionJeu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "date_debut")
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Column(name = "score_global")
    private Integer scoreGlobal;

    @Column(name = "score_base")
    private Integer scoreBase;

    @Column(name = "score_final")
    private Integer scoreFinal;

    @Column(name = "ai_adjustment")
    private Double aiAdjustment;

    @Column(name = "ai_adjustment_source", length = 40)
    private String aiAdjustmentSource;

    @Column(name = "ai_explanation_code", length = 80)
    private String aiExplanationCode;

    @Column(name = "xp_gained")
    private Integer xpGained;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "scoring_rules_version", length = 32)
    private String scoringRulesVersion;

    @Column(name = "anomaly_notes", columnDefinition = "TEXT")
    private String anomalyNotes;

    @Column(name = "accuracy_percent")
    private Integer accuracyPercent;

    @Column(name = "reaction_time_ms")
    private Integer reactionTimeMs;

    @Column(name = "total_questions")
    private Integer totalQuestions;

    @Column(name = "correct_answers")
    private Integer correctAnswers;

    @Column(name = "moves")
    private Integer moves;

    @Column(name = "matches_count")
    private Integer matchesCount;

    @Column(name = "attempts")
    private Integer attempts;

    @Column(name = "hints_used")
    private Integer hintsUsed;

    @Column(name = "total_rounds")
    private Integer totalRounds;

    @Column(name = "successful_rounds")
    private Integer successfulRounds;

    @Column(name = "mode_jeu_lance", length = 20)
    private String modeJeuLance;

    @Column(name = "room_code", length = 16)
    private String roomCode;

    @Column(name = "team_name", length = 80)
    private String teamName;

    @Column(name = "niveau_atteint")
    private Integer niveauAtteint;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_session", nullable = false)
    private EtatSession etatSession;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private User utilisateur;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_jeu", nullable = false)
    private Jeu jeu;
}

