package com.britechnology.edugame.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "questions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Question {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(columnDefinition = "TEXT")
    private String contenu;

    @Column(name = "bonne_reponse", columnDefinition = "TEXT")
    private String bonneReponse;
    
    @Column(name = "options", columnDefinition = "TEXT")
    private String options;

    @Column(name = "sous_type", length = 40)
    private String sousType;

    @Column(name = "media_url", columnDefinition = "TEXT")
    private String mediaUrl;

    @Column(name = "prompt_audio_url", columnDefinition = "TEXT")
    private String promptAudioUrl;

    @Column(columnDefinition = "TEXT")
    private String explication;

    private Integer difficulte;

    @ManyToOne(optional = false)
    @JoinColumn(name = "id_jeu", nullable = false)
    private Jeu jeu;
}

