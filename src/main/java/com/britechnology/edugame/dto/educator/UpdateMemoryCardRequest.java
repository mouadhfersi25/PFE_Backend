package com.britechnology.edugame.dto.educator;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body PUT /api/educator/memory-cards/:id. Frontend: UpdateMemoryCardRequest (symbole?, pairKey?, categorie?). Tous optionnels.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateMemoryCardRequest {

    private String symbole;
    private String cardType;
    private String cardValue;
    private String sousType;
    private String pairKey;
    private String categorie;
}
