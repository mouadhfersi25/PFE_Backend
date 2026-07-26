package com.stage.auth.authbackend.dto.sponsor;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RecompenseDTO {
    private Long id;
    private String nom;
    private String description;
    private Integer scoreMin;
    private String typeRecompense;
    private String sponsorNom;
    private Integer stockTotal;
    private Integer stockRemaining;
    private Integer distributedCount;
    private Double valeur;
    private String devise;
    private String partenaireNom;
    private String dateEvenement;
    private String lieuEvenement;
    private String modeRemise;
    private String instructionsRemise;
    private String imageUrl;
    private String status;
}
