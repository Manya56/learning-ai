package com.learningai.backend.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

/**
 * GroqConfig — WebClient base builder for Groq API.
 *
 * CHANGED: No longer injects a single api-key here.
 * Key selection is now handled per-request by GroqKeyPool in AiService.
 * This bean provides a base WebClient without auth headers —
 * AiService adds the Authorization header dynamically per call.
 */
@Configuration
public class GroqConfig {

    @Bean
    public WebClient groqWebClient() {
        return WebClient.builder()
                .baseUrl("https://api.groq.com")
                .defaultHeader("Content-Type", "application/json")
                .codecs(config -> config
                        .defaultCodecs()
                        .maxInMemorySize(2 * 1024 * 1024))
                .build();
    }
}