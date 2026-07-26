package com.britechnology.edugame.dto.sponsor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PubliciteDTO {
    private Long id;
    private String contenu;
    private String status;
    private String typePublicite;
    private String imageUrl;
    private Integer adDurationSeconds;
    private String ctaLabel;
    private String ctaUrl;
    private Double budgetUtilise;
    private Integer nbVues;
    private Integer nbClics;
    private String sponsorNom;
}
