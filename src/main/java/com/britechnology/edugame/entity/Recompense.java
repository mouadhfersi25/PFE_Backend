package com.britechnology.edugame.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "recompenses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Recompense {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String nom;

    @Column(length = 255)
    private String description;

    @Column(name = "score_min")
    private Integer scoreMin;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_recompense", nullable = false)
    private TypeRecompense typeRecompense;

    @Column(name = "date_creation")
    private LocalDate dateCreation;

    @Builder.Default
    @Column(name = "active", nullable = false)
    private Boolean active = true;

    /** Le sponsor (compte User avec Role.SPONSOR) propriétaire de cette récompense. */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "id_sponsor")
    private User sponsor;

    @PrePersist
    protected void onCreate() {
        if (this.dateCreation == null) {
            this.dateCreation = LocalDate.now();
        }
        if (this.active == null) {
            this.active = true;
        }
    }
}

