package com.nutriguide;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Punto de entrada principal del backend de NutriGuide.
 * Plataforma educativa de nutrición con asistencia inteligente basada en arquitectura RAG y pgvector.
 */
@SpringBootApplication
public class NutriGuideApplication {

    public static void main(String[] args) {
        SpringApplication.run(NutriGuideApplication.class, args);
    }
}

