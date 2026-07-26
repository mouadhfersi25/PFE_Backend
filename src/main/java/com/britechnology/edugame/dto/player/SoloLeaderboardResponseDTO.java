package com.britechnology.edugame.dto.player;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SoloLeaderboardResponseDTO {
    /** GLOBAL | COUNTRY | REGION */
    private String scope;
    private String scopeLabel;
    private List<SoloLeaderboardEntryDTO> entries;
    private Integer currentUserRank;
    private Long currentUserId;
    private int totalPlayers;
}
