package com.britechnology.edugame.dto.reclamation;

import com.britechnology.edugame.entity.MotifReclamation;
import com.britechnology.edugame.entity.StatutReclamation;
import com.britechnology.edugame.entity.TypeJeu;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class ReclamationDTO {
    private Long id;
    private Long gameId;
    private String gameTitle;
    private TypeJeu gameType;
    private Long sessionId;
    private Long playerId;
    private String playerPrenom;
    private String playerNom;
    private String playerEmail;
    private MotifReclamation motif;
    private String commentaire;
    private StatutReclamation statut;
    private String reponseAdmin;
    private Long adminId;
    private String adminPrenom;
    private String adminNom;
    private boolean traitee;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
