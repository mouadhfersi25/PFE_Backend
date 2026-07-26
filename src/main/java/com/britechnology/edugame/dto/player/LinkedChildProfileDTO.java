package com.britechnology.edugame.dto.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

/**
 * Profil joueur exposé au compte parent (sans jetons / secrets).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LinkedChildProfileDTO {
    private Long id;
    private String nom;
    private String prenom;
    private String email;
    private LocalDate dateDeNaissance;
    private String avatarUrl;
    private Integer niveau;
    private Integer scoreTotal;
    private Integer pointsExperience;
    private Integer xpToNextLevel;
    private Integer currentStreakDays;
    private Integer bestStreakDays;
    private Integer skillMath;
    private Integer skillLogic;
    private Integer skillMemory;
    private Integer skillReflex;
    private boolean onboardingCompleted;
}
