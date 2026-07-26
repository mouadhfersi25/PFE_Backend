package com.britechnology.edugame.dto.admin;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AdminGlobalMetricsDTO {
    /** Pourcentage de sessions terminées parmi toutes les sessions. */
    private double overallCompletionRatePercent;
    /** Joueurs distincts ayant une activité sur les 30 derniers jours. */
    private long activePlayers;
    /** Moyenne des durées totales par joueur (sessions terminées), en minutes. */
    private double avgPlaytimeMinutesPerUser;
}
