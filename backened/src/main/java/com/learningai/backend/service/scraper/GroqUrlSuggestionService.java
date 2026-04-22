package com.learningai.backend.service.scraper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningai.backend.config.ScraperConfig;
import com.learningai.backend.service.AiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class GroqUrlSuggestionService {

    private final AiService      aiService;
    private final ObjectMapper   objectMapper;

    // ─── Ask Groq which URLs to scrape for a topic/question ──────────────

    public List<String> suggestUrls(String userQuestion,
                                     String topicGoal,
                                     String conceptName) {

        String systemPrompt = """
                You are a web research assistant.
                Your job is to suggest the BEST URLs to scrape for learning content.
                
                Rules:
                - Suggest exactly %d URLs
                - Prefer authoritative, educational sources
                - URLs must be publicly accessible (no login required)
                - Prefer sites with clean text (avoid heavy JS sites)
                - For technical topics: prefer official docs, Wikipedia, Baeldung
                - For finance: prefer Investopedia, Reuters
                - For science: prefer Wikipedia, Khan Academy, Nature
                - For general topics: prefer Wikipedia, BBC, Britannica
                - Never suggest social media, Reddit, or paywalled sites
                
                Respond ONLY with a valid JSON array of URL strings.
                No explanation, no markdown, just the array.
                Example: ["https://...", "https://..."]
                """.formatted(ScraperConfig.MAX_URLS_PER_GROQ_CALL);

        String userMessage =
                "Topic: %s\nConcept: %s\nQuestion: %s\n\n"
                .formatted(topicGoal, conceptName, userQuestion) +
                "What are the best URLs to scrape for this?";

        try {
            String raw = aiService.call(systemPrompt, userMessage);
            return parseUrlArray(raw);
        } catch (Exception e) {
            log.error("Groq URL suggestion failed: {}", e.getMessage());
            // Fallback: build a Wikipedia URL as last resort
            return buildFallbackUrls(conceptName, topicGoal);
        }
    }

    // ─── Ask Groq if it can answer confidently from its own knowledge ─────

    public GroqDirectAnswer tryDirectAnswer(String userQuestion,
                                             String topicGoal,
                                             String conceptName) {

        String systemPrompt = """
                You are an expert educator on %s.
                Answer the question using your own knowledge.
                
                At the END of your answer, on a new line, write exactly:
                CONFIDENCE: HIGH   (if you are very sure)
                CONFIDENCE: MEDIUM (if somewhat sure)
                CONFIDENCE: LOW    (if uncertain or topic needs recent data)
                
                Be honest about your confidence level.
                If the question needs very recent data (last 6 months),
                always say CONFIDENCE: LOW.
                """.formatted(topicGoal);

        String userMessage = "Question about %s: %s"
                .formatted(conceptName, userQuestion);

        try {
            String raw = aiService.call(systemPrompt, userMessage);
            return parseDirectAnswer(raw);
        } catch (Exception e) {
            log.error("Groq direct answer failed: {}", e.getMessage());
            return GroqDirectAnswer.builder()
                    .answer("")
                    .confidence("LOW")
                    .build();
        }
    }

    // ─── Parse helpers ────────────────────────────────────────────────────

    private List<String> parseUrlArray(String raw) {
        try {
            String cleaned = raw
                .replaceAll("(?s)```json\\s*", "")
                .replaceAll("(?s)```\\s*", "")
                .trim();

            // Find the JSON array even if there's text around it
            int start = cleaned.indexOf('[');
            int end   = cleaned.lastIndexOf(']');
            if (start == -1 || end == -1) {
                return buildFallbackUrls("general", "general");
            }

            cleaned = cleaned.substring(start, end + 1);
            JsonNode root = objectMapper.readTree(cleaned);

            List<String> urls = new ArrayList<>();
            root.forEach(node -> {
                String url = node.asText().trim();
                if (url.startsWith("http") && urls.size() 
                       < ScraperConfig.MAX_URLS_PER_GROQ_CALL) {
                    urls.add(url);
                }
            });

            log.info("Groq suggested {} URLs", urls.size());
            return urls;

        } catch (Exception e) {
            log.error("Failed to parse URL array: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    private GroqDirectAnswer parseDirectAnswer(String raw) {
        String confidence = "MEDIUM";
        String answer     = raw;

        // Extract confidence line
        if (raw.contains("CONFIDENCE: HIGH")) {
            confidence = "HIGH";
            answer = raw.replace("CONFIDENCE: HIGH", "").trim();
        } else if (raw.contains("CONFIDENCE: LOW")) {
            confidence = "LOW";
            answer = raw.replace("CONFIDENCE: LOW", "").trim();
        } else if (raw.contains("CONFIDENCE: MEDIUM")) {
            confidence = "MEDIUM";
            answer = raw.replace("CONFIDENCE: MEDIUM", "").trim();
        }

        return GroqDirectAnswer.builder()
                .answer(answer)
                .confidence(confidence)
                .build();
    }

    private List<String> buildFallbackUrls(String conceptName,
                                            String topicGoal) {
        // Wikipedia is always a safe fallback
        String wikiQuery = (conceptName + " " + topicGoal)
                .replaceAll("\\s+", "_");
        return List.of(
            "https://en.wikipedia.org/wiki/" + wikiQuery,
            "https://en.wikipedia.org/wiki/" +
                conceptName.replaceAll("\\s+", "_")
        );
    }

    // ─── Inner DTO ────────────────────────────────────────────────────────

    @lombok.Data
    @lombok.Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class GroqDirectAnswer {
        private String answer;
        private String confidence; // HIGH / MEDIUM / LOW
    }
}