package com.stage.auth.authbackend.config;

import org.springframework.context.ApplicationContextInitializer;
import org.springframework.core.env.Environment;
import org.springframework.context.ConfigurableApplicationContext;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * S'exécute au tout début du démarrage Spring Boot et crée la base PostgreSQL
 * "edugame" si elle n'existe pas (évite l'erreur "la base de données n'existe pas").
 */
public class PostgresDatabaseCreator implements ApplicationContextInitializer<ConfigurableApplicationContext> {

    private static final Pattern JDBC_URL_PATTERN = Pattern.compile("jdbc:postgresql://([^:]+):(\\d+)/(.+)");

    @Override
    public void initialize(ConfigurableApplicationContext applicationContext) {
        Environment env = applicationContext.getEnvironment();
        String url = env.getProperty("spring.datasource.url");
        if (url == null || !url.contains("postgresql")) {
            return;
        }

        Matcher m = JDBC_URL_PATTERN.matcher(url.trim());
        if (!m.matches()) {
            return;
        }

        String host = m.group(1);
        String port = m.group(2);
        String databaseName = m.group(3).split("\\?")[0].trim();
        if (databaseName.isEmpty() || "postgres".equalsIgnoreCase(databaseName)) {
            return;
        }

        String postgresUrl = "jdbc:postgresql://" + host + ":" + port + "/postgres";
        String username = env.getProperty("spring.datasource.username", "postgres");
        String password = env.getProperty("spring.datasource.password", "");

        try {
            Class.forName("org.postgresql.Driver");
        } catch (ClassNotFoundException e) {
            return;
        }

        try (Connection conn = DriverManager.getConnection(postgresUrl, username, password);
             Statement stmt = conn.createStatement()) {

            try (ResultSet rs = stmt.executeQuery(
                    "SELECT 1 FROM pg_database WHERE datname = '" + databaseName.replace("'", "''") + "'")) {
                if (rs.next()) {
                    return; // la base existe déjà
                }
            }

            stmt.executeUpdate("CREATE DATABASE " + quoteIdentifier(databaseName));
            System.out.println("[PostgresDatabaseCreator] Base de données '" + databaseName + "' créée.");
        } catch (Exception e) {
            System.err.println("[PostgresDatabaseCreator] Impossible de créer la base '" + databaseName + "': " + e.getMessage());
        }
    }

    private static String quoteIdentifier(String name) {
        return "\"" + name.replace("\"", "\"\"") + "\"";
    }
}
