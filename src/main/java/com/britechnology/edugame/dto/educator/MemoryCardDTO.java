package com.britechnology.edugame.dto.educator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Réponse GET /api/educator/memory-cards?gameId= et POST/PUT. Frontend: MemoryCardDTO (id, jeuId, jeuTitre, symbole, pairKey, categorie).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemoryCardDTO {

    private Long id;
    private Long jeuId;
    private String jeuTitre;

    private String symbole;
    private String cardType;
    private String cardValue;
    private String sousType;
    private String pairKey;
    private String categorie;
}
