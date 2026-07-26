package com.britechnology.edugame.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Id;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Column;
import jakarta.persistence.Enumerated;
import jakarta.persistence.EnumType;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.FetchType;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String nom;

    @Column(nullable = false)
    private String prenom;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private LocalDate dateDeNaissance;

    @Column
    private String avatarUrl;

    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private EtatCompte etatCompte = EtatCompte.ACTIF;

    @Builder.Default
    @Column(nullable = false)
    private boolean enabled = false;

    // Gamification (uniquement JOUEUR) => champs nullable
    private Integer niveau;
    private Integer scoreTotal;
    private Integer pointsExperience;
    private Integer currentStreakDays;
    private Integer bestStreakDays;
    private LocalDate lastStreakDate;

    @ManyToOne
    @JoinColumn(name = "id_region")
    private Region region;

    /** Pour les JOUEUR : true = a complété pays/région */
    @Column(name = "onboarding_completed", nullable = false, columnDefinition = "boolean default false")
    @Builder.Default
    private boolean onboardingCompleted = false;

    @ManyToOne
    @JoinColumn(name = "id_genre")
    private Genre genre;

    /**
     * Pour les comptes {@link Role#JOUEUR} : tuteur (compte {@link Role#PARENT}).
     * Null si le joueur n'est pas rattaché. Les comptes PARENT n'utilisent pas ce lien comme enfant.
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_parent")
    private User parent;

    private String resetToken;
    private LocalDateTime resetTokenExpiry;

    private String tokenVerification;
    private LocalDateTime dateExpirationToken;

    private LocalDateTime dateDerniereConnexion;

    @Column(updatable = false)
    private LocalDateTime dateCreation;

    @PrePersist
    protected void onCreate() {
        this.dateCreation = LocalDateTime.now();
        if (this.etatCompte == null) this.etatCompte = EtatCompte.ACTIF;

        // init gamification selon rôle
        if (this.role == Role.JOUEUR) {
            if (this.niveau == null) this.niveau = 1;
            if (this.scoreTotal == null) this.scoreTotal = 0;
            if (this.pointsExperience == null) this.pointsExperience = 0;
            if (this.currentStreakDays == null) this.currentStreakDays = 0;
            if (this.bestStreakDays == null) this.bestStreakDays = 0;
        } else {
            this.niveau = null;
            this.scoreTotal = null;
            this.pointsExperience = null;
            this.currentStreakDays = null;
            this.bestStreakDays = null;
            this.lastStreakDate = null;
            this.parent = null;
        }
    }

    @PreUpdate
    protected void onUpdateClearParentIfNotJoueur() {
        if (this.role != null && this.role != Role.JOUEUR) {
            this.parent = null;
        }
    }
}

