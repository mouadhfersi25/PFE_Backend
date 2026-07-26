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
public class PlayerBadgeOverviewItemDTO {
    private Long id;
    private String nom;
    private String description;
    private String icone;
    private String unlockCondition;
    private boolean earned;
    private boolean claimable;
    private LocalDate earnedDate;
}
