package com.britechnology.edugame.dto.reclamation;

import com.britechnology.edugame.entity.StatutReclamation;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UpdateReclamationRequest {
    private StatutReclamation statut;
    private String reponseAdmin;
}
