package com.britechnology.edugame;

import com.britechnology.edugame.config.PostgresDatabaseCreator;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EdugameApplication {

    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(EdugameApplication.class);
        app.addInitializers(new PostgresDatabaseCreator());
        app.run(args);
    }

}