package com.britechnology.edugame.dto.sponsor;

import lombok.*;

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
    /** Conservé pour compatibilité frontend (contient l'URL vidéo). */
    private String imageUrl;
    private String videoUrl;
    private Integer adDurationSeconds;
    private String ctaLabel;
    private String ctaUrl;
    private Integer nbVues;
    private Integer nbClics;
    private Long sponsorId;
    private String sponsorNom;
    private String sponsorEmail;
    private java.util.List<Long> jeuIds;
    private java.util.List<String> jeuTitres;
}
