package com.learningai.backend.config;

import lombok.Getter;
import org.springframework.context.annotation.Configuration;

import java.util.Set;

@Configuration(proxyBeanMethods = false)
@Getter
public class ScraperConfig {

    // Domains we trust and actively scrape from
    public static final Set<String> TRUSTED_DOMAINS = Set.of(
        "geeksforgeeks.org",
        "leetcode.com",
        "programiz.com",
        "javatpoint.com",
        "cs50.harvard.edu",
        "en.wikipedia.org",
        "cp-algorithms.com",
        "baeldung.com",
        "medium.com",
        "dev.to",
        "towardsdatascience.com",
        "khanacademy.org",
        "coursera.org",
        "developer.mozilla.org",
        "docs.oracle.com",
        "spring.io",
        "stackoverflow.com",
        "github.com",
        "arxiv.org",
        "sciencedirect.com",
        "britannica.com",
        "healthline.com",
        "investopedia.com",
        "reuters.com",
        "bbc.com",
        "nature.com"
    );

    // Domains we never scrape
    public static final Set<String> BLOCKED_DOMAINS = Set.of(
        "facebook.com",
        "twitter.com",
        "instagram.com",
        "tiktok.com",
        "pinterest.com",
        "reddit.com" // too noisy
    );

    // Scraping behavior
    public static final int    REQUEST_TIMEOUT_MS      = 10_000;
    public static final int    MAX_BODY_LENGTH_CHARS   = 50_000;
    public static final int    MIN_BODY_LENGTH_CHARS   = 200;
    public static final int    RATE_LIMIT_DELAY_MS     = 1_500;
    public static final int    MAX_URLS_PER_GROQ_CALL  = 4;
    public static final int    MAX_RETRIES             = 2;
    public static final String USER_AGENT =
        "LearnAI-Bot/1.0 (Educational content indexing; " +
        "contact: admin@learnai.com)";
}