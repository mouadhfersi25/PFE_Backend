package com.britechnology.edugame.dto.player;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Body admin : rattacher ou détacher un joueur d'un parent.
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class SetParentLinkRequest {
    /** Identifiant du compte PARENT, ou null pour retirer le lien. */
    private Long parentId;
}
