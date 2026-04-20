// package com.learningai.backend.config;

// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.context.annotation.Bean;
// import org.springframework.context.annotation.Configuration;
// import org.springframework.web.reactive.function.client.WebClient;

// @Configuration
// public class ClaudeConfig {

//     @Value("${claude.api-key}")
//     private String apiKey;

//     @Bean
//     public WebClient claudeWebClient() {
//         return WebClient.builder()
//                 .baseUrl("https://api.anthropic.com")
//                 .defaultHeader("x-api-key", apiKey)
//                 .defaultHeader("anthropic-version", "2023-06-01")
//                 .defaultHeader("content-type", "application/json")
//                 .codecs(config -> config
//                         .defaultCodecs()
//                         .maxInMemorySize(2 * 1024 * 1024))
//                 .build();
//     }
// }