package com.britechnology.edugame.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "sessions_oral")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionOral {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_utilisateur", nullable = false)
    private User utilisateur;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_series", nullable = false)
    private VoiceSeries series;

    @Column(name = "date_debut", nullable = false)
    private LocalDateTime dateDebut;

    @Column(name = "date_fin")
    private LocalDateTime dateFin;

    @Column(name = "duration_seconds")
    private Integer durationSeconds;

    @Column(name = "score_base")
    private Integer scoreBase;

    @Column(name = "score_final")
    private Integer scoreFinal;

    @Column(name = "xp_gained")
    private Integer xpGained;

    @Column(name = "accuracy_percent")
    private Integer accuracyPercent;

    @Column(name = "prompts_total")
    private Integer promptsTotal;

    @Column(name = "prompts_reussis")
    private Integer promptsReussis;

    @Column(name = "niveau_atteint")
    private Integer niveauAtteint;

    @Column(name = "scoring_rules_version", length = 32)
    private String scoringRulesVersion;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat_session", nullable = false, length = 20)
    @Builder.Default
    private EtatSessionOral etatSession = EtatSessionOral.EN_COURS;
}
