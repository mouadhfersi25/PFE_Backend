package com.britechnology.edugame.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "voice_prompts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VoicePrompt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "series_id", nullable = false)
    private VoiceSeries series;

    @Column(nullable = false)
    @Builder.Default
    private Integer ordre = 0;

    @Column(name = "texte_reference", nullable = false, columnDefinition = "TEXT")
    private String texteReference;

    @Enumerated(EnumType.STRING)
    @Column(name = "sous_type", nullable = false, length = 30)
    @Builder.Default
    private VoicePromptSousType sousType = VoicePromptSousType.READ_ALOUD;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    @Builder.Default
    private VoiceTolerance tolerance = VoiceTolerance.NORMAL;

    @Column(columnDefinition = "TEXT")
    private String indice;

    @Column(name = "duree_max_secondes", nullable = false)
    @Builder.Default
    private Integer dureeMaxSecondes = 30;
}
