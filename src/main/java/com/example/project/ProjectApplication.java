package com.example.project;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import jakarta.annotation.PostConstruct;
import java.util.TimeZone;

@SpringBootApplication
@EnableScheduling
@EnableAsync
public class ProjectApplication {

    @PostConstruct
    public void init() {
        // Configurar zona horaria por defecto para toda la aplicación
        TimeZone.setDefault(TimeZone.getTimeZone("America/Lima"));
        System.out.println("🕒 Zona horaria configurada: " + TimeZone.getDefault().getID());
        System.out.println("🕒 Hora actual del sistema: " + java.time.LocalDateTime.now());
    }

    public static void main(String[] args) {
        SpringApplication.run(ProjectApplication.class, args);
    }
}