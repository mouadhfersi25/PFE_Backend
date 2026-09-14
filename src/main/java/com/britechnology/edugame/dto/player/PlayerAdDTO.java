package com.britechnology.edugame.dto.player;

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
public class PlayerAdDTO {
    private Long id;
    private String contenu;
    private String videoUrl;
    private Integer adDurationSeconds;
    private String ctaLabel;
    private String ctaUrl;
    private String sponsorNom;
}
