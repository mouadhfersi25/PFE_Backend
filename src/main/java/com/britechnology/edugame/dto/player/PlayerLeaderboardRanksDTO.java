package com.britechnology.edugame.dto.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerLeaderboardRanksDTO {
    private Integer globalRank;
    private int globalTotal;

    private boolean countryRankingAvailable;
    private Integer countryRank;
    private int countryTotal;
    private Long paysId;
    private String paysNom;

    private boolean regionRankingAvailable;
    private Integer regionRank;
    private int regionTotal;
    private Long regionId;
    private String regionNom;
}
