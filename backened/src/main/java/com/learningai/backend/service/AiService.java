package com.learningai.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningai.backend.dto.response.EvaluationResult;
import com.learningai.backend.dto.response.ProblemResponse;
import com.learningai.backend.dto.response.QuizQuestionResponse;
import com.learningai.backend.entity.QuizSession;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.util.retry.Retry;

import java.time.Duration;
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

        // ── Retry / timeout constants ─────────────────────────────────────────
        private static final int MAX_RETRIES = 3;
        private static final long RETRY_BACKOFF_MS = 1_000;
        private static final int TIMEOUT_SECONDS = 30;

        // ─── Core call — with retry + timeout ────────────────────────────────

        public String call(String systemPrompt, String userMessage) {
                return callWithRetry(systemPrompt, userMessage, 2_000, 0.7);
        }

        public String callWithHistory(String systemPrompt,
                        List<Map<String, Object>> messages) {
                return callHistoryWithRetry(systemPrompt, messages, 2_000, 0.8);
        }

        // ─── Internal retry wrapper (call) ────────────────────────────────────

        private String callWithRetry(String systemPrompt,
                        String userMessage,
                        int maxTokens,
                        double temperature) {

                Map<String, Object> body = Map.of(
                                "model", model,
                                "max_tokens", maxTokens,
                                "temperature", temperature,
                                "messages", List.of(
                                                Map.of("role", "system", "content", systemPrompt),
                                                Map.of("role", "user", "content", userMessage)));

                for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                        try {
                                JsonNode response = groqWebClient
                                                .post()
                                                .uri("/openai/v1/chat/completions")
                                                .bodyValue(body)
                                                .retrieve()
                                                .bodyToMono(JsonNode.class)
                                                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                                                .block();

                                return extractContent(response);

                        } catch (WebClientResponseException.TooManyRequests e) {
                                // 429 — must back off
                                log.warn("Groq rate limit hit (attempt {}/{}), backing off {}ms",
                                                attempt, MAX_RETRIES, RETRY_BACKOFF_MS * attempt);
                                sleep(RETRY_BACKOFF_MS * attempt);

                        } catch (WebClientResponseException e) {
                                // 4xx (other than 429) — don't retry, fail immediately
                                log.error("Groq HTTP {} on attempt {}: {}",
                                                e.getStatusCode(), attempt, e.getResponseBodyAsString());
                                throw new RuntimeException("Groq API error: " + e.getMessage());

                        } catch (Exception e) {
                                if (attempt == MAX_RETRIES) {
                                        log.error("Groq call failed after {} attempts: {}", MAX_RETRIES,
                                                        e.getMessage());
                                        throw new RuntimeException(
                                                        "Groq API unavailable after retries: " + e.getMessage());
                                }
                                log.warn("Groq call failed (attempt {}/{}): {} — retrying in {}ms",
                                                attempt, MAX_RETRIES, e.getMessage(), RETRY_BACKOFF_MS * attempt);
                                sleep(RETRY_BACKOFF_MS * attempt);
                        }
                }
                throw new RuntimeException("Groq API failed after " + MAX_RETRIES + " attempts");
        }

        // ─── Internal retry wrapper (history call) ────────────────────────────

        private String callHistoryWithRetry(String systemPrompt,
                        List<Map<String, Object>> messages,
                        int maxTokens,
                        double temperature) {

                List<Map<String, Object>> fullMessages = buildMessagesWithSystem(systemPrompt, messages);

                Map<String, Object> body = Map.of(
                                "model", model,
                                "max_tokens", maxTokens,
                                "temperature", temperature,
                                "messages", fullMessages);

                for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
                        try {
                                JsonNode response = groqWebClient
                                                .post()
                                                .uri("/openai/v1/chat/completions")
                                                .bodyValue(body)
                                                .retrieve()
                                                .bodyToMono(JsonNode.class)
                                                .timeout(Duration.ofSeconds(TIMEOUT_SECONDS))
                                                .block();

                                return extractContent(response);

                        } catch (WebClientResponseException.TooManyRequests e) {
                                log.warn("Groq rate limit hit on history call (attempt {}/{})", attempt, MAX_RETRIES);
                                sleep(RETRY_BACKOFF_MS * attempt);

                        } catch (WebClientResponseException e) {
                                log.error("Groq HTTP {} on history call: {}", e.getStatusCode(), e.getMessage());
                                throw new RuntimeException("Groq API error: " + e.getMessage());

                        } catch (Exception e) {
                                if (attempt == MAX_RETRIES) {
                                        log.error("Groq history call failed after {} attempts: {}", MAX_RETRIES,
                                                        e.getMessage());
                                        throw new RuntimeException("Groq API unavailable: " + e.getMessage());
                                }
                                log.warn("Groq history call failed (attempt {}/{}): {}", attempt, MAX_RETRIES,
                                                e.getMessage());
                                sleep(RETRY_BACKOFF_MS * attempt);
                        }
                }
                throw new RuntimeException("Groq history call failed after " + MAX_RETRIES + " attempts");
        }

        // ─── Extract text content from Groq response ──────────────────────────

        private String extractContent(JsonNode response) {
                if (response == null) {
                        throw new RuntimeException("Groq returned null response");
                }

                JsonNode choices = response.path("choices");
                if (choices.isEmpty() || !choices.isArray()) {
                        // Log full response for debugging
                        log.error("Groq response has no choices: {}", response);
                        throw new RuntimeException("Groq returned empty choices");
                }

                String content = choices.get(0)
                                .path("message")
                                .path("content")
                                .asText();

                if (content == null || content.isBlank()) {
                        throw new RuntimeException("Groq returned empty content");
                }

                return content;
        }

        // public String call(String systemPrompt, String userMessage) {
        // try

        // {
        // Map<String, Object> body = Map.of(
        // "model", model,
        // "max_tokens", 2000,
        // "temperature", 0.7,
        // "messages", List.of(
        // Map.of("role", "system", "content", systemPrompt),
        // Map.of("role", "user", "content", userMessage)));

        // JsonNode response = groqWebClient
        // .post()
        // .uri("/openai/v1/chat/completions")
        // .bodyValue(body)
        // .retrieve()
        // .bodyToMono(JsonNode.class)
        // .block();

        // return response
        // .path("choices")
        // .get(0)
        // .path("message")
        // .path("content")
        // .asText();

        // } catch (Exception e) {
        // log.error("Groq API call failed: {}", e.getMessage());
        // throw new RuntimeException("Groq API call failed: " + e.getMessage());
        // }
        // }

        // ─── Generate prior knowledge quiz ───────────────────────────────────

        public List<QuizQuestionResponse> generateKnowledgeQuiz(
                        String goal, int priorLevel) {

                String systemPrompt = """
                                You are an expert educator and assessment designer.
                                Generate exactly 5 multiple choice questions to assess
                                a student's current knowledge level on the given topic.

                                Rules:
                                - Questions must be relevant to the specific goal topic
                                - Mix easy, medium, and hard questions
                                - Each question has exactly 4 options
                                - Only one correct answer per question
                                - Provide a brief explanation for the correct answer
                                - Questions should work for ANY learning domain (tech, finance, art, science, etc.)

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
                                goal, priorLevel);

                String rawResponse = callWithRetry(systemPrompt, userMessage, 2_000, 0.7);
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
                                - Topics must be relevant to the specific goal
                                - This should work for ANY domain: tech, business, arts, sports, cooking, etc.

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
                                goal, goalDescription, difficulty);

                String rawResponse = callWithRetry(systemPrompt, userMessage, 1_000, 0.7);
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
                        log.error("Failed to parse quiz JSON: {}\nRaw: {}", e.getMessage(), raw);
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
                        log.error("Failed to parse topics JSON: {}\nRaw: {}", e.getMessage(), raw);
                        throw new RuntimeException("Failed to parse roadmap response");
                }
        }

        private String cleanJson(String raw) {
                return raw.replaceAll("(?s)```json\\s*", "")
                                .replaceAll("(?s)```\\s*", "")
                                .trim();
        }

        public List<QuizSession.QuizQuestionData> generateQuizForConcept(
                        String conceptName,
                        String difficulty,
                        String learningStyle,
                        int count) {

                String styleInstruction = switch (learningStyle) {
                        case "PRACTICE" -> "Focus on application and hands-on problems. Be concise.";
                        case "READING" -> "Include questions that test conceptual understanding with detailed context.";
                        case "VISUAL" -> "Use scenario-based questions with step-by-step examples.";
                        default -> "Mix conceptual and application questions.";
                };

                String systemPrompt = """
                                You are an expert educator and quiz generator.
                                Generate exactly %d multiple choice questions on the given concept.

                                Rules:
                                - Questions must test deep understanding, not just memorization
                                - Each question has exactly 4 options
                                - Only one correct answer
                                - Include 3 progressive hints (each gives a bit more away)
                                - Hint 1: conceptual nudge. Hint 2: approach hint. Hint 3: near-answer.
                                - Include a clear explanation for the correct answer
                                - Style guide: %s
                                - This should work for ANY topic: DSA, stocks, music, cooking, etc.

                                IMPORTANT: Respond ONLY with valid JSON. No markdown, no extra text.
                                {
                                  "questions": [
                                    {
                                      "questionNumber": 1,
                                      "question": "...",
                                      "options": ["A", "B", "C", "D"],
                                      "correctAnswerIndex": 0,
                                      "explanation": "...",
                                      "hints": ["hint1", "hint2", "hint3"]
                                    }
                                  ]
                                }
                                """.formatted(count, styleInstruction);

                String userMessage = "Generate %d %s difficulty questions on: %s"
                                .formatted(count, difficulty, conceptName);

                String raw = callWithRetry(systemPrompt, userMessage, 3_000, 0.7);
                return parseConceptQuizResponse(raw);
        }

        // ─── Generate a contextual hint ──────────────────────────────────────

        public String generateHint(String question,
                        String conceptName,
                        int hintNumber) {
                String systemPrompt = """
                                You are a helpful tutor. Give hint #%d for this question.
                                - Hint 1: conceptual nudge only
                                - Hint 2: point toward the approach
                                - Hint 3: almost the answer, but don't reveal it completely
                                Keep it under 2 sentences. Do not repeat previous hints.
                                Respond with ONLY the hint text, no labels, no formatting.
                                """.formatted(hintNumber);

                String userMessage = "Question: " + question +
                                "\nConcept: " + conceptName;

                return callWithRetry(systemPrompt, userMessage, 300, 0.6).trim();
        }

        // ─── Parse helper ─────────────────────────────────────────────────────

        private List<QuizSession.QuizQuestionData> parseConceptQuizResponse(String raw) {
                try {
                        String cleaned = cleanJson(raw);
                        JsonNode root = objectMapper.readTree(cleaned);
                        List<QuizSession.QuizQuestionData> questions = new ArrayList<>();

                        for (JsonNode q : root.path("questions")) {
                                List<String> options = new ArrayList<>();
                                q.path("options").forEach(o -> options.add(o.asText()));

                                List<String> hints = new ArrayList<>();
                                q.path("hints").forEach(h -> hints.add(h.asText()));

                                questions.add(QuizSession.QuizQuestionData.builder()
                                                .questionNumber(q.path("questionNumber").asInt())
                                                .question(q.path("question").asText())
                                                .options(options)
                                                .correctAnswerIndex(q.path("correctAnswerIndex").asInt())
                                                .explanation(q.path("explanation").asText())
                                                .hints(hints)
                                                .build());
                        }
                        return questions;

                } catch (Exception e) {
                        log.error("Failed to parse concept quiz JSON: {}", e.getMessage());
                        throw new RuntimeException("Failed to parse quiz response");
                }
        }

        // ─── Generate a practice problem (topic-agnostic) ─────────────────────

        public ProblemResponse generateProblem(
                        String conceptName,
                        String topicGoal,
                        String difficulty,
                        String learningStyle) {

                String problemType = inferProblemType(topicGoal);

                String styleGuide = switch (learningStyle) {
                        case "PRACTICE" -> "Give a direct hands-on problem. Minimal explanation.";
                        case "READING" -> "Include context and background before the problem.";
                        case "VISUAL" -> "Use a scenario or real-world analogy to frame the problem.";
                        default -> "Give a clear, direct problem statement.";
                };

                String typeGuide = switch (problemType) {
                        case "CODING" -> """
                                        Include:
                                        - problem_statement: clear description
                                        - starter_code: skeleton in the requested language
                                        - test_cases: array of {input, expected_output} (3 cases)
                                        - constraints: time/space complexity hints
                                        """;
                        case "MATH" -> """
                                        Include:
                                        - problem_statement: the mathematical problem
                                        - starter_code: show the formula structure or steps to fill
                                        - test_cases: array of {input, expected_output} (2 cases)
                                        - constraints: allowed methods or theorems
                                        """;
                        default -> """
                                        Include:
                                        - problem_statement: a thought-provoking question or scenario
                                        - starter_code: a response template or outline to fill in
                                        - test_cases: array of {criteria, expected_quality} (2 evaluation criteria)
                                        - constraints: word limit or key points that must be covered
                                        """;
                };

                String systemPrompt = """
                                You are an expert educator creating practice problems.

                                Generate ONE practice problem for the given concept.

                                Style: %s
                                Problem format: %s
                                Difficulty: %s

                                STRICT OUTPUT RULES:
                                - Return ONLY valid JSON
                                - DO NOT use markdown (no ```json)
                                - DO NOT include explanations outside JSON
                                - ALL string values must be SINGLE LINE
                                - Replace line breaks with \\n inside strings
                                - Ensure JSON is parsable by Jackson
                                - Do NOT include unescaped newline characters

                                RESPONSE FORMAT:
                                {
                                  "problem_statement": "single line string",
                                  "problem_type": "%s",
                                  "language": "java",
                                  "starter_code": "single line string",
                                  "test_cases": [
                                    {"input": "single line", "expected_output": "single line"}
                                  ],
                                  "constraints": "single line string",
                                  "hints": ["hint1", "hint2", "hint3"]
                                }
                                """.formatted(styleGuide, typeGuide, difficulty, problemType);

                String userMessage = "Topic: %s. Concept: %s. Difficulty: %s."
                                .formatted(topicGoal, conceptName, difficulty);

                String raw = callWithRetry(systemPrompt, userMessage, 2_000, 0.7);
                return parseProblemResponse(raw, conceptName, difficulty, problemType);
        }

        // ─── Evaluate a user's submission (topic-agnostic) ────────────────────

        public EvaluationResult evaluateSubmission(
                        String problemStatement,
                        String userSubmission,
                        String problemType,
                        String language,
                        String conceptName) {

                String evaluationGuide = switch (problemType) {
                        case "CODING" -> """
                                        Evaluate the code for:
                                        1. Correctness — does it solve the problem?
                                        2. Time complexity — is it optimal?
                                        3. Edge cases — handled properly?
                                        4. Code quality — clean, readable?
                                        5. Test case results — which pass/fail?
                                        """;
                        case "MATH" -> """
                                        Evaluate the solution for:
                                        1. Correct method/formula used?
                                        2. Steps shown clearly?
                                        3. Final answer correct?
                                        4. Edge cases considered?
                                        """;
                        default -> """
                                        Evaluate the written response for:
                                        1. Does it address the core question?
                                        2. Accuracy of facts/concepts?
                                        3. Clarity and structure?
                                        4. Depth of understanding shown?
                                        5. Key points covered?
                                        """;
                };

                String systemPrompt = """
                                You are an expert evaluator and educator.
                                Evaluate the student's submission fairly but rigorously.

                                %s

                                Score 0-10 where:
                                10 = perfect, 8-9 = excellent, 6-7 = good,
                                4-5 = partial, 2-3 = poor, 0-1 = incorrect/blank

                                IMPORTANT: Respond ONLY with valid JSON. No markdown.
                                {
                                  "score": 7,
                                  "passed": true,
                                  "strengths": "What they did well",
                                  "issues": "What was wrong or missing",
                                  "suggestions": "How to improve",
                                  "corrected_solution": "The ideal solution",
                                  "line_feedback": [
                                    {"line": "specific line or point", "comment": "feedback"}
                                  ]
                                }
                                """.formatted(evaluationGuide);

                String userMessage = """
                                Problem: %s

                                Student's %s submission:
                                %s

                                Concept being tested: %s
                                """.formatted(problemStatement, language, userSubmission, conceptName);

                String raw = callWithRetry(systemPrompt, userMessage, 2_000, 0.5);
                return parseEvaluationResult(raw);
        }

        // ─── Infer problem type from topic goal ───────────────────────────────

        private String inferProblemType(String topicGoal) {
                if (topicGoal == null)
                        return "WRITTEN";
                String lower = topicGoal.toLowerCase();

                boolean isCoding = lower.contains("dsa") ||
                                lower.contains("algorithm") ||
                                lower.contains("programming") ||
                                lower.contains("java") ||
                                lower.contains("python") ||
                                lower.contains("spring") ||
                                lower.contains("coding") ||
                                lower.contains("data structure") ||
                                lower.contains("system design") ||
                                lower.contains("kotlin") ||
                                lower.contains("javascript") ||
                                lower.contains("typescript");

                boolean isMath = lower.contains("math") ||
                                lower.contains("calculus") ||
                                lower.contains("statistics") ||
                                lower.contains("physics") ||
                                lower.contains("linear algebra") ||
                                lower.contains("probability") ||
                                lower.contains("quantitative");

                if (isCoding)
                        return "CODING";
                if (isMath)
                        return "MATH";
                return "WRITTEN";
        }

        // ─── Parse helpers ────────────────────────────────────────────────────

        private ProblemResponse parseProblemResponse(String raw,
                        String conceptName,
                        String difficulty,
                        String problemType) {
                try {
                        String cleaned = cleanJson(raw);
                        JsonNode root = objectMapper.readTree(cleaned);

                        List<ProblemResponse.TestCase> testCases = new ArrayList<>();
                        for (JsonNode tc : root.path("test_cases")) {
                                testCases.add(ProblemResponse.TestCase.builder()
                                                .input(tc.path("input").asText())
                                                .expectedOutput(tc.path("expected_output")
                                                                .asText(tc.path("expected_quality").asText()))
                                                .build());
                        }

                        List<String> hints = new ArrayList<>();
                        root.path("hints").forEach(h -> hints.add(h.asText()));

                        return ProblemResponse.builder()
                                        .conceptName(conceptName)
                                        .difficulty(difficulty)
                                        .problemType(root.path("problem_type").asText(problemType))
                                        .language(root.path("language").asText("text"))
                                        .problemStatement(root.path("problem_statement").asText())
                                        .starterCode(root.path("starter_code").asText())
                                        .testCases(testCases)
                                        .constraints(root.path("constraints").asText())
                                        .hints(hints)
                                        .build();

                } catch (Exception e) {
                        log.error("Failed to parse problem response: {}", e.getMessage());
                        throw new RuntimeException("Failed to parse problem from AI");
                }
        }

        private EvaluationResult parseEvaluationResult(String raw) {
                try {
                        String cleaned = cleanJson(raw);
                        JsonNode root = objectMapper.readTree(cleaned);

                        List<EvaluationResult.LineFeedback> lineFeedback = new ArrayList<>();
                        for (JsonNode lf : root.path("line_feedback")) {
                                lineFeedback.add(EvaluationResult.LineFeedback.builder()
                                                .line(lf.path("line").asText())
                                                .comment(lf.path("comment").asText())
                                                .build());
                        }

                        return EvaluationResult.builder()
                                        .score(root.path("score").asInt())
                                        .passed(root.path("passed").asBoolean())
                                        .strengths(root.path("strengths").asText())
                                        .issues(root.path("issues").asText())
                                        .suggestions(root.path("suggestions").asText())
                                        .correctedSolution(root.path("corrected_solution").asText())
                                        .lineFeedback(lineFeedback)
                                        .build();

                } catch (Exception e) {
                        log.error("Failed to parse evaluation result: {}", e.getMessage());
                        throw new RuntimeException("Failed to parse evaluation from AI");
                }
        }

        // Add this method to AiService.java:

        // public String callWithHistory(String systemPrompt,
        //                 List<Map<String, Object>> messages) {
        //         try {
        //                 Map<String, Object> body = Map.of(
        //                                 "model", model,
        //                                 "max_tokens", 2000,
        //                                 "temperature", 0.8,
        //                                 "messages", buildMessagesWithSystem(systemPrompt, messages));

        //                 JsonNode response = groqWebClient
        //                                 .post()
        //                                 .uri("/openai/v1/chat/completions")
        //                                 .bodyValue(body)
        //                                 .retrieve()
        //                                 .bodyToMono(JsonNode.class)
        //                                 .block();

        //                 return response
        //                                 .path("choices")
        //                                 .get(0)
        //                                 .path("message")
        //                                 .path("content")
        //                                 .asText();

        //         } catch (Exception e) {
        //                 log.error("Groq history call failed: {}", e.getMessage());
        //                 throw new RuntimeException("Groq call failed: " + e.getMessage());
        //         }
        // }

        private List<Map<String, Object>> buildMessagesWithSystem(
                        String systemPrompt,
                        List<Map<String, Object>> history) {
                List<Map<String, Object>> all = new ArrayList<>();
                all.add(Map.of("role", "system", "content", systemPrompt));
                all.addAll(history);
                return all;
        }

        private void sleep(long ms) {
                try {
                        Thread.sleep(ms);
                } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                }
        }
}