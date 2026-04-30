package com.learningai.backend.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;


@Configuration
public class CorsConfig implements WebMvcConfigurer {

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/api/**")
                .allowedOriginPatterns(
                        "http://localhost:*",     // local dev
                            "http://localhost:5173",  // Vite dev server
                                "http://localhost:3000",  // fallback
                        "https://*.railway.app",  // Railway production
                        "https://*.render.com",   // Render production
                        "https://*.vercel.app"    // Vercel frontend if used
                )
                .allowedMethods("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS")
                .allowedHeaders(
                        "Authorization", "Content-Type", "Accept",
                        "X-Requested-With", "Origin"
                )
                .allowCredentials(true)
                .maxAge(3600);
    }
}