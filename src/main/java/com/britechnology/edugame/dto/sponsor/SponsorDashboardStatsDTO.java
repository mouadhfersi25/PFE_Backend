package com.britechnology.edugame.dto.sponsor;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SponsorDashboardStatsDTO {
    private Integer activeCampaigns;
    private Integer totalImpressions;
    private Integer totalClicks;
    private Double ctr;
    private Integer distributedRewards;
    private Integer rewardStock;
}
