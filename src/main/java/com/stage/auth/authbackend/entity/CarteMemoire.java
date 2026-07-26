package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "cartes_memoire")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CarteMemoire {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Jeu de type MEMOIRE auquel cette carte appartient.
     */
    @ManyToOne(optional = false)
    @JoinColumn(name = "id_jeu", nullable = false)
    private Jeu jeu;

    /**
     * Symbole affiché sur la carte (emoji, texte, etc.). Aligné frontend / DTO.
     */
    @Column(nullable = false, length = 100)
    private String symbole;

    /**
     * Type de contenu affiché sur la carte: EMOJI, TEXT, IMAGE, COLOR.
     */
    @Column(name = "card_type", length = 20)
    private String cardType;

    /**
     * Valeur brute du contenu (texte, URL image, code couleur, etc.).
     */
    @Column(name = "card_value", columnDefinition = "TEXT")
    private String cardValue;

    /**
     * Sous-type pédagogique du jeu mémoire.
     */
    @Column(name = "sous_type", length = 40)
    private String sousType;

    /**
     * Identifiant de paire pour regrouper deux cartes qui vont ensemble.
     */
    @Column(name = "pair_key", length = 50)
    private String pairKey;

    /**
     * Catégorie ou thème de la carte (optionnel).
     */
    @Column(length = 100)
    private String categorie;
}

