package com.learningai.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningai.backend.dto.response.QuizQuestionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final WebClient groqWebClient;
    private final ObjectMapper objectMapper;

    @Value("${groq.model}")
    private String model;

    // ─── Core call method ─────────────────────────────────────────────────

    public String call(String systemPrompt, String userMessage) {
        try {
            Map<String, Object> body = Map.of(
                "model", model,
                "max_tokens", 2000,
                "temperature", 0.7,
                "messages", List.of(
                    Map.of("role", "system", "content", systemPrompt),
                    Map.of("role", "user",   "content", userMessage)
                )
            );

            JsonNode response = groqWebClient
                    .post()
                    .uri("/openai/v1/chat/completions")
                    .bodyValue(body)
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            return response
                    .path("choices")
                    .get(0)
                    .path("message")
                    .path("content")
                    .asText();

        } catch (Exception e) {
            log.error("Groq API call failed: {}", e.getMessage());
            throw new RuntimeException("Groq API call failed: " + e.getMessage());
        }
    }

    // ─── Generate prior knowledge quiz ───────────────────────────────────

    public List<QuizQuestionResponse> generateKnowledgeQuiz(
            String goal, int priorLevel) {

        String systemPrompt = """
                You are an expert educator and assessment designer.
                Generate exactly 5 multiple choice questions to assess
                a student's current knowledge level.
                
                Rules:
                - Questions must be relevant to the goal topic
                - Mix easy, medium, and hard questions
                - Each question has exactly 4 options
                - Only one correct answer per question
                - Provide a brief explanation for the correct answer
                
                IMPORTANT: Respond ONLY with valid JSON.
                No extra text, no markdown, no code blocks.
                Use this exact format:
                {
                  "questions": [
                    {
                      "questionNumber": 1,
                      "question": "question text here",
                      "options": ["option A", "option B", "option C", "option D"],
                      "correctAnswerIndex": 0,
                      "explanation": "why this answer is correct"
                    }
                  ]
                }
                """;

        String userMessage = String.format(
                "Generate a 5-question knowledge assessment for: %s. " +
                "Student self-reported level: %d/3 " +
                "(1=beginner, 2=some knowledge, 3=intermediate).",
                goal, priorLevel
        );

        String rawResponse = call(systemPrompt, userMessage);
        return parseQuizResponse(rawResponse);
    }

    // ─── Generate roadmap topics ──────────────────────────────────────────

    public List<String> generateRoadmapTopics(
            String goal, String difficulty, String goalDescription) {

        String systemPrompt = """
                You are an expert curriculum designer.
                Generate a structured learning roadmap as an ordered list.
                
                Rules:
                - Return exactly 10 topics
                - Order from fundamentals to advanced
                - Each topic is concise (2-5 words)
                - Topics must be relevant to the goal
                
                IMPORTANT: Respond ONLY with valid JSON.
                No extra text, no markdown, no code blocks.
                Use this exact format:
                {
                  "topics": [
                    "Topic 1",
                    "Topic 2"
                  ]
                }
                """;

        String userMessage = String.format(
                "Learning goal: %s. Description: %s. Difficulty: %s.",
                goal, goalDescription, difficulty
        );

        String rawResponse = call(systemPrompt, userMessage);
        return parseTopicsResponse(rawResponse);
    }

    // ─── Parse helpers ────────────────────────────────────────────────────

    private List<QuizQuestionResponse> parseQuizResponse(String raw) {
        try {
            String cleaned = cleanJson(raw);
            JsonNode root = objectMapper.readTree(cleaned);
            List<QuizQuestionResponse> questions = new ArrayList<>();

            for (JsonNode q : root.path("questions")) {
                List<String> options = new ArrayList<>();
                q.path("options").forEach(o -> options.add(o.asText()));

                questions.add(QuizQuestionResponse.builder()
                        .questionNumber(q.path("questionNumber").asInt())
                        .question(q.path("question").asText())
                        .options(options)
                        .correctAnswerIndex(q.path("correctAnswerIndex").asInt())
                        .explanation(q.path("explanation").asText())
                        .build());
            }
            return questions;

        } catch (Exception e) {
            log.error("Failed to parse quiz JSON: {}\nRaw: {}", 
                    e.getMessage(), raw);
            throw new RuntimeException("Failed to parse quiz response");
        }
    }

    private List<String> parseTopicsResponse(String raw) {
        try {
            String cleaned = cleanJson(raw);
            JsonNode root = objectMapper.readTree(cleaned);
            List<String> topics = new ArrayList<>();
            root.path("topics").forEach(t -> topics.add(t.asText()));
            return topics;

        } catch (Exception e) {
            log.error("Failed to parse topics JSON: {}\nRaw: {}",
                    e.getMessage(), raw);
            throw new RuntimeException("Failed to parse roadmap response");
        }
    }

    private String cleanJson(String raw) {
        // Remove markdown code fences if model adds them
        return raw.replaceAll("(?s)```json\\s*", "")
                  .replaceAll("(?s)```\\s*", "")
                  .trim();
    }
}