package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "jeux")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Jeu {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String titre;

    @Column(columnDefinition = "TEXT")
    private String description;

    private Integer difficulte;

    @Column(name = "age_min")
    private Integer ageMin;

    @Column(name = "age_max")
    private Integer ageMax;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_jeu", nullable = false)
    private TypeJeu typeJeu;

    @Enumerated(EnumType.STRING)
    @Column(name = "mode_jeu", nullable = false)
    private ModeJeu modeJeu;

    @Column(name = "actif")
    @Builder.Default
    private boolean actif = false;

    @Column(name = "duree_minutes")
    private Integer dureeMinutes;

    @Column(name = "icone", length = 20)
    private String icone;

    @Column(name = "cover_image_url", columnDefinition = "TEXT")
    private String coverImageUrl;

    @Column(name = "date_creation")
    private LocalDateTime dateCreation;

    /**
     * Date de dernière modification du contenu pédagogique du jeu
     * (questions, cartes mémoire, puzzles, paramètres réflexe...).
     * Ne doit pas être utilisée pour les métadonnées générales.
     */
    @Column(name = "last_content_update_at")
    private LocalDateTime lastContentUpdateAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "etat")
    @Builder.Default
    private EtatJeu etat = EtatJeu.BROUILLON;

    /**
     * Éducateur créateur du jeu (nullable pour les jeux créés directement par l'admin).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_educateur")
    private User educateur;
}
