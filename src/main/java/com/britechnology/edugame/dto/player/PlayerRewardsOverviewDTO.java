package com.britechnology.edugame.dto.player;

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
public class PlayerRewardsOverviewDTO {
    private int total;
    private int claimable;
    private int claimed;
    private List<PlayerRewardOverviewItemDTO> rewards;
}
