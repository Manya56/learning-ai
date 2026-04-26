package com.learningai.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class LanguageService {

    private final AiService    aiService;
    private final ObjectMapper objectMapper;

    // ─── Detect language of text ──────────────────────────────────────────

    public DetectionResult detect(String text) {
        if (text == null || text.isBlank()) {
            return new DetectionResult("en", "English", true);
        }

        // Quick ASCII check — if >90% ASCII it's almost certainly English
        long nonAscii = text.chars()
                .filter(c -> c > 127)
                .count();
        double nonAsciiRatio = (double) nonAscii / text.length();

        if (nonAsciiRatio < 0.05 && !containsNonEnglishPatterns(text)) {
            return new DetectionResult("en", "English", true);
        }

        // Ask Groq to detect
        try {
            String systemPrompt = """
                    You are a language detector.
                    Respond ONLY with valid JSON. No explanation, no markdown.
                    Format: {"code": "hi", "name": "Hindi", "is_english": false}
                    Language codes: en=English, hi=Hindi, es=Spanish,
                    fr=French, de=German, zh=Chinese, ar=Arabic,
                    pt=Portuguese, ru=Russian, ja=Japanese, ko=Korean,
                    te=Telugu, ta=Tamil, bn=Bengali, mr=Marathi,
                    gu=Gujarati, kn=Kannada, pa=Punjabi, ur=Urdu
                    """;

            String userMessage = "Detect the language of: " + text;
            String raw = aiService.call(systemPrompt, userMessage)
                    .replaceAll("(?s)```json\\s*", "")
                    .replaceAll("(?s)```\\s*", "")
                    .trim();

            JsonNode node = objectMapper.readTree(raw);
            String code      = node.path("code").asText("en");
            String name      = node.path("name").asText("English");
            boolean isEng    = node.path("is_english").asBoolean(false)
                    || "en".equals(code);

            log.info("Language detected: {} ({})", name, code);
            return new DetectionResult(code, name, isEng);

        } catch (Exception e) {
            log.warn("Language detection failed: {} — defaulting to English",
                    e.getMessage());
            return new DetectionResult("en", "English", true);
        }
    }

    // ─── Translate text to English ────────────────────────────────────────

    public String translateToEnglish(String text, String sourceLangName) {
        if (text == null || text.isBlank()) return text;

        try {
            String systemPrompt = String.format("""
                    You are a professional translator.
                    Translate the following %s text to English.
                    Rules:
                    - Translate ONLY — do not answer or explain
                    - Preserve technical terms, names, and proper nouns
                    - Keep the same meaning and intent
                    - Return ONLY the translated text, nothing else
                    """, sourceLangName);

            String translated = aiService.call(systemPrompt, text).trim();
            log.info("Translated from {} to English: '{}'→'{}'",
                    sourceLangName,
                    text.substring(0, Math.min(50, text.length())),
                    translated.substring(0, Math.min(50, translated.length())));

            return translated;

        } catch (Exception e) {
            log.warn("Translation to English failed: {}", e.getMessage());
            return text; // return original if translation fails
        }
    }

    // ─── Translate answer back to user's language ─────────────────────────

    public String translateFromEnglish(String englishText,
                                        String targetLangCode,
                                        String targetLangName) {
        if (englishText == null || englishText.isBlank()) return englishText;
        if ("en".equals(targetLangCode)) return englishText;

        try {
            String systemPrompt = String.format("""
                    You are a professional translator.
                    Translate the following English text to %s.
                    Rules:
                    - Translate ONLY — do not add explanations
                    - Preserve technical terms in English (e.g. "RAG", "DSA",
                      "array", "algorithm") — do not translate them
                    - Keep code examples in English — never translate code
                    - Maintain the same tone and structure
                    - Return ONLY the translated text
                    """, targetLangName);

            String translated = aiService.call(
                    systemPrompt, englishText).trim();

            log.info("Translated answer to {}", targetLangName);
            return translated;

        } catch (Exception e) {
            log.warn("Translation to {} failed: {}",
                    targetLangName, e.getMessage());
            return englishText; // return English if translation fails
        }
    }

    // ─── Full pipeline: detect + translate to English ─────────────────────

    public TranslationContext prepareQuery(String userInput) {
        DetectionResult detection = detect(userInput);

        String englishQuery = detection.isEnglish()
                ? userInput
                : translateToEnglish(userInput, detection.languageName());

        return new TranslationContext(
                userInput,
                englishQuery,
                detection.languageCode(),
                detection.languageName(),
                detection.isEnglish()
        );
    }

    // ─── Helper ───────────────────────────────────────────────────────────

    private boolean containsNonEnglishPatterns(String text) {
        // Check for common Devanagari (Hindi/Marathi), Arabic, CJK ranges
        return text.codePoints().anyMatch(cp ->
                (cp >= 0x0900 && cp <= 0x097F) || // Devanagari
                (cp >= 0x0600 && cp <= 0x06FF) || // Arabic
                (cp >= 0x4E00 && cp <= 0x9FFF) || // CJK
                (cp >= 0xAC00 && cp <= 0xD7AF) || // Korean
                (cp >= 0x3040 && cp <= 0x309F)    // Japanese Hiragana
        );
    }

    // ─── Inner records ────────────────────────────────────────────────────

    public record DetectionResult(
            String languageCode,
            String languageName,
            boolean isEnglish) {}

    public record TranslationContext(
            String originalQuery,
            String englishQuery,
            String languageCode,
            String languageName,
            boolean wasEnglish) {}
}