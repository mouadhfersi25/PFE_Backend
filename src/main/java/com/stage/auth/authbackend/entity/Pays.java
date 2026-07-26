package com.stage.auth.authbackend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "pays")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Pays {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 150)
    private String nom;

    @Column(name = "code_iso", nullable = false, length = 10)
    private String codeIso;
}

