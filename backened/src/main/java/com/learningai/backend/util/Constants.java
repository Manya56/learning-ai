package com.learningai.backend.util;

public final class Constants {

    private Constants() {}

    // Cache key prefixes
    public static final String CACHE_QUIZ       = "quiz:";
    public static final String CACHE_USER        = "user:";
    public static final String CACHE_ROADMAP     = "roadmap:";
    public static final String CACHE_REVISION    = "revision:";

    // JWT
    public static final String BEARER_PREFIX     = "Bearer ";
    public static final String AUTH_HEADER        = "Authorization";

    // Difficulty levels
    public static final String DIFFICULTY_EASY   = "EASY";
    public static final String DIFFICULTY_MEDIUM = "MEDIUM";
    public static final String DIFFICULTY_HARD   = "HARD";

    // Learning styles
    public static final String STYLE_VISUAL      = "VISUAL";
    public static final String STYLE_READING      = "READING";
    public static final String STYLE_PRACTICE     = "PRACTICE";

    // API prefixes
    public static final String API_V1            = "/api/v1";
}
