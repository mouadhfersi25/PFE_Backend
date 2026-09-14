package com.britechnology.edugame.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

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

    @Column(nullable = false, length = 500)
    private String contenu;

    @Column(name = "video_url", nullable = false, columnDefinition = "TEXT")
    private String videoUrl;

    @Builder.Default
    @Column(name = "type_publicite", nullable = false, length = 50)
    private String typePublicite = "VIDEO";

    @Builder.Default
    @Column(name = "ad_duration_seconds", nullable = false)
    private Integer adDurationSeconds = 8;

    @Column(name = "cta_label", length = 120)
    private String ctaLabel;

    @Column(name = "cta_url", columnDefinition = "TEXT")
    private String ctaUrl;

    @Builder.Default
    @Column(name = "nb_vues", nullable = false)
    private Integer nbVues = 0;

    @Builder.Default
    @Column(name = "nb_clics", nullable = false)
    private Integer nbClics = 0;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "id_createur", nullable = false)
    private User createur;

    @Column(name = "date_creation", nullable = false)
    private LocalDateTime dateCreation;

    @Column(name = "date_maj")
    private LocalDateTime dateMaj;

    @Builder.Default
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "publicite_jeux",
            joinColumns = @JoinColumn(name = "publicite_id"),
            inverseJoinColumns = @JoinColumn(name = "jeu_id")
    )
    private Set<Jeu> jeux = new HashSet<>();

    @PrePersist
    protected void onCreate() {
        if (dateCreation == null) {
            dateCreation = LocalDateTime.now();
        }
        if (typePublicite == null || typePublicite.isBlank()) {
            typePublicite = "VIDEO";
        }
        if (adDurationSeconds == null) {
            adDurationSeconds = 8;
        }
        if (nbVues == null) {
            nbVues = 0;
        }
        if (nbClics == null) {
            nbClics = 0;
        }
        if (active == null) {
            active = true;
        }
        if (jeux == null) {
            jeux = new HashSet<>();
        }
    }

    @PreUpdate
    protected void onUpdate() {
        dateMaj = LocalDateTime.now();
    }
}
