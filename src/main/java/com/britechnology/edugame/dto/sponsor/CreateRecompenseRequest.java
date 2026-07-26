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
public class CreateRecompenseRequest {
    private String nom;
    private String description;
    private Integer scoreMin;
    private String typeRecompense;
}
