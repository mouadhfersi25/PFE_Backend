package com.stage.auth.authbackend.config;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.annotation.Order;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Au démarrage, modifie la colonne users.avatar_url en TEXT si nécessaire,
 * pour pouvoir stocker les images base64 (data URL) choisies à l'inscription.
 */
@Component
@Order(100)
public class AvatarUrlColumnMigration implements ApplicationRunner {

    private final JdbcTemplate jdbcTemplate;

    public AvatarUrlColumnMigration(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(ApplicationArguments args) {
        try {
            jdbcTemplate.execute(
                "ALTER TABLE users ALTER COLUMN avatar_url TYPE TEXT USING avatar_url::TEXT"
            );
        } catch (Exception e) {
            // Colonne déjà en TEXT ou table inexistante : ignorer
        }
    }
}
