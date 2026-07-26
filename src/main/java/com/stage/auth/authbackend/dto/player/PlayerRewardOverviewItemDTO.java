package com.stage.auth.authbackend.dto.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PlayerRewardOverviewItemDTO {
    private Long id;
    private Long claimId;
    private String nom;
    private String description;
    private String typeRecompense;
    private Integer scoreMin;
    private String unlockCondition;
    private boolean claimable;
    private boolean claimed;
    private String requestStatus;
    private LocalDate requestedDate;
}
