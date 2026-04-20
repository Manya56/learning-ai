// package com.learningai.backend.service;

// import com.fasterxml.jackson.databind.JsonNode;
// import com.fasterxml.jackson.databind.ObjectMapper;
// import com.learningai.backend.dto.response.QuizQuestionResponse;
// import lombok.RequiredArgsConstructor;
// import lombok.extern.slf4j.Slf4j;
// import org.springframework.beans.factory.annotation.Value;
// import org.springframework.stereotype.Service;
// import org.springframework.web.reactive.function.client.WebClient;

// import java.util.ArrayList;
// import java.util.List;
// import java.util.Map;

// @Slf4j
// @Service
// @RequiredArgsConstructor
// public class ClaudeApiService {

//     private final WebClient claudeWebClient;
//     private final ObjectMapper objectMapper;

//     @Value("${claude.model}")
//     private String model;

//     // ─── Core call method ───────────────────────────────────────────────────

//     public String call(String systemPrompt, String userMessage) {
//         try {
//             Map<String, Object> body = Map.of(
//                     "model", model,
//                     "max_tokens", 2000,
//                     "system", systemPrompt,
//                     "messages", List.of(
//                             Map.of("role", "user", "content", userMessage)
//                     )
//             );

//             JsonNode response = claudeWebClient
//                     .post()
//                     .uri("/v1/messages")
//                     .bodyValue(body)
//                     .retrieve()
//                     .bodyToMono(JsonNode.class)
//                     .block();

//             return response
//                     .path("content")
//                     .get(0)
//                     .path("text")
//                     .asText();

//         } catch (Exception e) {
//             log.error("Claude API call failed: {}", e.getMessage());
//             throw new RuntimeException("Claude API call failed: " + e.getMessage());
//         }
//     }

//     // ─── Generate prior knowledge quiz ──────────────────────────────────────

//     public List<QuizQuestionResponse> generateKnowledgeQuiz(
//             String goal, int priorLevel) {

//         String systemPrompt = """
//                 You are an expert educator and assessment designer.
//                 Generate exactly 5 multiple choice questions to assess
//                 a student's current knowledge level.
                
//                 Rules:
//                 - Questions must be relevant to the goal topic
//                 - Mix easy, medium, and hard questions
//                 - Each question has exactly 4 options (A, B, C, D)
//                 - Only one correct answer per question
//                 - Provide a brief explanation for the correct answer
                
//                 Respond ONLY with valid JSON in this exact format,
//                 no extra text before or after:
//                 {
//                   "questions": [
//                     {
//                       "questionNumber": 1,
//                       "question": "question text here",
//                       "options": ["option A", "option B", "option C", "option D"],
//                       "correctAnswerIndex": 0,
//                       "explanation": "why this answer is correct"
//                     }
//                   ]
//                 }
//                 """;

//         String userMessage = String.format(
//                 "Generate a 5-question knowledge assessment quiz for: %s. " +
//                 "The student's self-reported knowledge level is %d/3 " +
//                 "(1=beginner, 2=some knowledge, 3=intermediate).",
//                 goal, priorLevel
//         );

//         String rawResponse = call(systemPrompt, userMessage);

//         try {
//             // Strip markdown code blocks if Claude adds them
//             String cleaned = rawResponse
//                     .replaceAll("```json", "")
//                     .replaceAll("```", "")
//                     .trim();

//             JsonNode root = objectMapper.readTree(cleaned);
//             JsonNode questionsNode = root.path("questions");

//             List<QuizQuestionResponse> questions = new ArrayList<>();
//             for (JsonNode q : questionsNode) {
//                 List<String> options = new ArrayList<>();
//                 q.path("options").forEach(o -> options.add(o.asText()));

//                 questions.add(QuizQuestionResponse.builder()
//                         .questionNumber(q.path("questionNumber").asInt())
//                         .question(q.path("question").asText())
//                         .options(options)
//                         .correctAnswerIndex(q.path("correctAnswerIndex").asInt())
//                         .explanation(q.path("explanation").asText())
//                         .build());
//             }
//             return questions;

//         } catch (Exception e) {
//             log.error("Failed to parse quiz response: {}", e.getMessage());
//             throw new RuntimeException("Failed to parse quiz from Claude");
//         }
//     }

//     // ─── Generate initial roadmap topics ────────────────────────────────────

//     public List<String> generateRoadmapTopics(
//             String goal, String difficulty, String goalDescription) {

//         String systemPrompt = """
//                 You are an expert curriculum designer.
//                 Generate a structured learning roadmap as an ordered
//                 list of topics.
                
//                 Rules:
//                 - Return 8 to 12 topics maximum
//                 - Order from fundamentals to advanced
//                 - Each topic should be concise (2-5 words)
//                 - Topics must be directly relevant to the goal
                
//                 Respond ONLY with valid JSON in this exact format:
//                 {
//                   "topics": [
//                     "Topic 1",
//                     "Topic 2",
//                     "Topic 3"
//                   ]
//                 }
//                 """;

//         String userMessage = String.format(
//                 "Create a learning roadmap for: %s. " +
//                 "Goal: %s. " +
//                 "Starting difficulty: %s.",
//                 goal, goalDescription, difficulty
//         );

//         String rawResponse = call(systemPrompt, userMessage);

//         try {
//             String cleaned = rawResponse
//                     .replaceAll("```json", "")
//                     .replaceAll("```", "")
//                     .trim();

//             JsonNode root = objectMapper.readTree(cleaned);
//             List<String> topics = new ArrayList<>();
//             root.path("topics").forEach(t -> topics.add(t.asText()));
//             return topics;

//         } catch (Exception e) {
//             log.error("Failed to parse roadmap response: {}", e.getMessage());
//             throw new RuntimeException("Failed to parse roadmap from Claude");
//         }
//     }
// }

// eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiVVNFUiIsInR5cGUiOiJhY2Nlc3MiLCJ1c2VySWQiOiJkOWJmYTdlNi1jMzY2LTRiNDItYWUyYS00OGM3YmZkYTI0ODMiLCJzdWIiOiJ0ZXN0QHRlc3QuY29tIiwiaWF0IjoxNzc2NjY4ODg5LCJleHAiOjE3NzY3NTUyODl9.RBVXPYqJ65QkGMmyuvNrnoigSbN3G6pVM40uZhHNrRixWwAD_j6UoEphEQCFyVcZKojDztvTe-5J9DnkzNf8zQ
// curl -X POST http://localhost:8080/api/onboarding/quiz \
//   -H "Content-Type: application/json" \
//   -H "Authorization: eyJhbGciOiJIUzUxMiJ9.eyJyb2xlIjoiVVNFUiIsInR5cGUiOiJhY2Nlc3MiLCJ1c2VySWQiOiJkOWJmYTdlNi1jMzY2LTRiNDItYWUyYS00OGM3YmZkYTI0ODMiLCJzdWIiOiJ0ZXN0QHRlc3QuY29tIiwiaWF0IjoxNzc2NjY5MTkzLCJleHAiOjE3NzY3NTU1OTN9.4XZVRK9UaDa90BTLQR_j5HjdK-tHhuVUmdeMxY9lJoaJwomqxTA5-n2GE5Su3SAZTYBuu94caaPiN2tvsdQL9Q" \
//   -d "{\"goal\":\"DSA\",\"goalDescription\":\"Crack FAANG interviews\",\"preferredLanguage\":\"English\",\"priorKnowledgeLevel\":1}"