package com.britechnology.edugame.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "interactions_publicite")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionPublicite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "publicite_id", nullable = false)
    private Publicite publicite;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "utilisateur_id", nullable = false)
    private User utilisateur;

    @Column(name = "session_id")
    private Long sessionId;

    @Column(name = "type_interaction", nullable = false, length = 20)
    private String typeInteraction;

    @Column(name = "date_interaction", nullable = false)
    private LocalDateTime dateInteraction;

    @PrePersist
    protected void onCreate() {
        if (dateInteraction == null) {
            dateInteraction = LocalDateTime.now();
        }
    }
}
