package com.britechnology.edugame.dto.sponsor;

import lombok.*;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class SponsorDashboardStatsDTO {
    private Integer totalCampaigns;
    private Integer activeCampaigns;
    private Integer pausedCampaigns;
    private Integer totalImpressions;
    private Integer totalClicks;
    private Integer distributedRewards;
    private Integer rewardStock;
    private Integer pendingRewardRequests;
}
