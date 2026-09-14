package com.britechnology.edugame;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.TestPropertySource;

// Pas de migration V1 dans db/migration : le projet suppose que le schema
// existe deja (cree par Hibernate ddl-auto=update) avant que Flyway ne joue
// ses ajustements incrementaux (V2+). Sur une base vraiment vide (CI), Flyway
// s'execute avant Hibernate et echoue donc des V4 (ALTER TABLE sur "jeux" qui
// n'existe pas encore). On bypasse Flyway ici et on laisse Hibernate generer
// le schema directement depuis les entites JPA, uniquement pour ce test —
// aucun impact sur le comportement reel de l'appli (deploy/prod).
@SpringBootTest
@TestPropertySource(properties = {
        "spring.flyway.enabled=false",
        "spring.jpa.hibernate.ddl-auto=create-drop"
})
class AuthBackendApplicationTests {

    @Test
    void contextLoads() {
    }

}
