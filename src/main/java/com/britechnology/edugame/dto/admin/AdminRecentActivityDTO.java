package com.britechnology.edugame.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminRecentActivityDTO {
    private String id;
    private String player;
    /** Libellé affichable (ex. Terminé, En cours). */
    private String action;
    private String game;
    /** Instant ISO-8601. */
    private String occurredAt;
}
