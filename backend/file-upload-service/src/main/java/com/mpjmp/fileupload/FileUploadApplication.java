package com.mpjmp.fileupload;

import io.github.cdimascio.dotenv.Dotenv;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class FileUploadApplication {

    // Static block to load .env and inject as system properties BEFORE Spring context
    static {
        Dotenv dotenv = Dotenv.configure()
            .directory("../../") // Corrected to point to project root
            .ignoreIfMissing()
            .load();
        dotenv.entries().forEach(entry -> {
            if (System.getProperty(entry.getKey()) == null) {
                System.setProperty(entry.getKey(), entry.getValue());
            }
        });
    }

    public static void main(String[] args) {
        SpringApplication.run(FileUploadApplication.class, args);
    }

    @Bean
    public Dotenv dotenv() {
        return Dotenv.configure()
                .directory("../../") // Corrected to point to project root
                .ignoreIfMissing()
                .load();
    }
}