package com.stage.auth.authbackend;

import com.stage.auth.authbackend.config.PostgresDatabaseCreator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class AuthBackendApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(AuthBackendApplication.class);
        app.addInitializers(new PostgresDatabaseCreator());
        app.run(args);
    }

}