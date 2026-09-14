package com.britechnology.edugame.dto.sponsor;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdatePubliciteRequest {
    private String contenu;
    private String videoUrl;
    private String imageUrl;
    private Integer adDurationSeconds;
    private String ctaLabel;
    private String ctaUrl;
    private List<Long> jeuIds;
}
