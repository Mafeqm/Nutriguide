package com.nutriguide.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * Registro de los interceptores de seguridad de la API.
 */
@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final AdminTokenInterceptor adminTokenInterceptor;
    private final ChatRateLimitInterceptor chatRateLimitInterceptor;

    public WebConfig(AdminTokenInterceptor adminTokenInterceptor, ChatRateLimitInterceptor chatRateLimitInterceptor) {
        this.adminTokenInterceptor = adminTokenInterceptor;
        this.chatRateLimitInterceptor = chatRateLimitInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(adminTokenInterceptor)
                .addPathPatterns("/api/alimentos/**", "/api/recetas/**", "/api/documentos/**");
        registry.addInterceptor(chatRateLimitInterceptor)
                .addPathPatterns("/api/chat/**");
    }
}
