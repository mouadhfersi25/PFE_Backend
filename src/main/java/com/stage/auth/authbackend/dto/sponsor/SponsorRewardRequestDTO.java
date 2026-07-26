package com.stage.auth.authbackend.dto.sponsor;

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
public class SponsorRewardRequestDTO {
    private Long id;
    private Long rewardId;
    private String rewardName;
    private Integer rewardScoreMin;
    private Long playerId;
    private String playerName;
    private String playerEmail;
    private Integer playerScoreTotal;
    private String status;
    private LocalDate requestedDate;
}
