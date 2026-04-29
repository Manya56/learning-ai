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

    private static final int MAX_RETRIES = 3;
    private static final long RETRY_BACKOFF_MS = 1_000;
    private static final int TIMEOUT_SECONDS = 30;

    // ─── Core call ────────────────────────────────────────────────────────

    public String call(String systemPrompt, String userMessage) {
        return callWithRetry(systemPrompt, userMessage, 2_000, 0.7);
    }

    public String callWithHistory(String systemPrompt,
            List<Map<String, Object>> messages) {
        return callHistoryWithRetry(systemPrompt, messages, 2_000, 0.8);
    }

    // ─── Internal retry wrappers ──────────────────────────────────────────

    private String callWithRetry(String systemPrompt, String userMessage,
            int maxTokens, double temperature) {

        Map<String, Object> body = Map.of(
                "model", model, "max_tokens", maxTokens,
                "temperature", temperature,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userMessage)));

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                JsonNode response = groqWebClient.post()
                        .uri("/openai/v1/chat/completions").bodyValue(body)
                        .retrieve().bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS)).block();
                return extractContent(response);
            } catch (WebClientResponseException.TooManyRequests e) {
                log.warn("Groq rate limit (attempt {}/{})", attempt, MAX_RETRIES);
                sleep(RETRY_BACKOFF_MS * attempt);
            } catch (WebClientResponseException e) {
                throw new RuntimeException("Groq API error: " + e.getMessage());
            } catch (Exception e) {
                if (attempt == MAX_RETRIES)
                    throw new RuntimeException("Groq unavailable after retries: " + e.getMessage());
                sleep(RETRY_BACKOFF_MS * attempt);
            }
        }
        throw new RuntimeException("Groq failed after " + MAX_RETRIES + " attempts");
    }

    private String callHistoryWithRetry(String systemPrompt,
            List<Map<String, Object>> messages, int maxTokens, double temperature) {

        List<Map<String, Object>> full = buildMessagesWithSystem(systemPrompt, messages);
        Map<String, Object> body = Map.of("model", model, "max_tokens", maxTokens,
                "temperature", temperature, "messages", full);

        for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
            try {
                JsonNode response = groqWebClient.post()
                        .uri("/openai/v1/chat/completions").bodyValue(body)
                        .retrieve().bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS)).block();
                return extractContent(response);
            } catch (WebClientResponseException.TooManyRequests e) {
                sleep(RETRY_BACKOFF_MS * attempt);
            } catch (WebClientResponseException e) {
                throw new RuntimeException("Groq API error: " + e.getMessage());
            } catch (Exception e) {
                if (attempt == MAX_RETRIES)
                    throw new RuntimeException("Groq unavailable: " + e.getMessage());
                sleep(RETRY_BACKOFF_MS * attempt);
            }
        }
        throw new RuntimeException("Groq history call failed");
    }

    private String extractContent(JsonNode response) {
        if (response == null)
            throw new RuntimeException("Groq returned null");
        JsonNode choices = response.path("choices");
        if (choices.isEmpty())
            throw new RuntimeException("Groq returned empty choices: " + response);
        String content = choices.get(0).path("message").path("content").asText();
        if (content == null || content.isBlank())
            throw new RuntimeException("Groq returned empty content");
        return content;
    }

    // ─── Knowledge quiz ───────────────────────────────────────────────────

    public List<QuizQuestionResponse> generateKnowledgeQuiz(String goal, int priorLevel) {
        String systemPrompt = """
                You are an expert educator. Generate exactly 5 MCQ questions
                to assess a student's knowledge on the given topic.
                Works for ANY domain: tech, finance, art, music, science, etc.

                ACCURACY: Verify each correct answer before including it.
                Only include questions you are 100%% certain about.

                Respond ONLY with valid JSON. No markdown.
                {
                  "questions": [
                    {
                      "questionNumber": 1,
                      "question": "...",
                      "options": ["A","B","C","D"],
                      "correctAnswerIndex": 0,
                      "explanation": "..."
                    }
                  ]
                }
                """;
        return parseQuizResponse(callWithRetry(systemPrompt,
                "Topic: %s. Student level: %d/3.".formatted(goal, priorLevel), 2_000, 0.3));
    }

    // ─── Roadmap topics ───────────────────────────────────────────────────

    public List<String> generateRoadmapTopics(String goal, String difficulty, String desc) {
        String systemPrompt = """
                You are a curriculum designer. Generate 10 ordered learning topics
                for the given goal. Works for ANY domain.
                Respond ONLY with valid JSON: { "topics": ["Topic 1", "Topic 2"] }
                """;
        return parseTopicsResponse(callWithRetry(systemPrompt,
                "Goal: %s. Description: %s. Difficulty: %s.".formatted(goal, desc, difficulty),
                1_000, 0.7));
    }

    // ─── Quiz for a concept ───────────────────────────────────────────────
    // FIX Issue 5: temperature lowered to 0.3 for factual accuracy
    // Added explicit "verify before answering" instruction

    public List<QuizSession.QuizQuestionData> generateQuizForConcept(
            String conceptName,
            String topicGoal, // NEW — the parent domain/goal
            String difficulty,
            String learningStyle,
            int count) {

        String styleInstruction = switch (learningStyle) {
            case "PRACTICE" -> "Focus on application. Be concise.";
            case "READING" -> "Test conceptual understanding with detailed context.";
            case "VISUAL" -> "Use scenario-based questions with step-by-step examples.";
            default -> "Mix conceptual and application questions.";
        };

        // KEY FIX: topicGoal is injected into the system prompt so the AI
        // knows which domain it is working in before seeing the concept name.
        String domainContext = (topicGoal != null && !topicGoal.isBlank())
                ? topicGoal
                : "the given subject";

        String systemPrompt = """
                You are an expert educator specializing in: %s

                Your job is to generate quiz questions about a SPECIFIC CONCEPT
                that is part of this domain. All questions must be firmly grounded
                in the domain of "%s" — not any other field.

                ACCURACY IS CRITICAL:
                - Verify each correct answer is factually accurate within the domain of "%s"
                - If unsure about a question's answer, skip it and write another
                - The correctAnswerIndex MUST point to the TRULY correct option
                - Wrong options must be clearly incorrect — no ambiguous questions

                Generate exactly %d MCQ questions.
                Difficulty: %s
                Style guide: %s

                Respond ONLY with valid JSON. No markdown, no extra text.
                {
                  "questions": [
                    {
                      "questionNumber": 1,
                      "question": "...",
                      "options": ["A","B","C","D"],
                      "correctAnswerIndex": 0,
                      "explanation": "clear factual explanation within the %s domain",
                      "hints": ["hint1","hint2","hint3"]
                    }
                  ]
                }
                """.formatted(
                domainContext, domainContext, domainContext,
                count, difficulty, styleInstruction, domainContext);

        // User message also includes topicGoal for extra clarity
        String userMessage = "Domain: %s\nConcept: %s (a sub-topic within %s)\nDifficulty: %s\n\n"
                .formatted(domainContext, conceptName, domainContext, difficulty) +
                "Generate %d quiz questions about '%s' specifically in the context of %s."
                        .formatted(count, conceptName, domainContext);

        // Low temperature = factual accuracy
        String raw = callWithRetry(systemPrompt, userMessage, 3_000, 0.3);
        return parseConceptQuizResponse(raw);
    }

    // ─── Hint generation ─────────────────────────────────────────────────

    public String generateHint(String question, String conceptName, int hintNumber) {
        String systemPrompt = """
                Give hint #%d. Hint 1=nudge, Hint 2=approach, Hint 3=near-answer.
                Under 2 sentences. Only hint text, no labels.
                """.formatted(hintNumber);
        return callWithRetry(systemPrompt,
                "Q: " + question + "\nConcept: " + conceptName, 300, 0.6).trim();
    }

    // ─── Generate practice problem ────────────────────────────────────────
    // FIX Issue 2: Finance/math topics now get CALCULATION type, not CODING
    // Learning style PRACTICE no longer forces coding problems

    public ProblemResponse generateProblem(String conceptName, String topicGoal,
            String difficulty, String learningStyle) {

        // FIX: problem type is based on TOPIC, not learning style
        String problemType = inferProblemType(topicGoal, conceptName);

        String styleGuide = switch (learningStyle) {
            case "PRACTICE" -> "Direct hands-on problem. Minimal explanation.";
            case "READING" -> "Include context before the problem.";
            case "VISUAL" -> "Use a real-world scenario.";
            default -> "Clear, direct problem statement.";
        };

        String typeGuide = switch (problemType) {
            case "CODING" -> """
                    - problem_statement: description of the coding problem
                    - starter_code: code skeleton (use \\n for newlines)
                    - test_cases: [{input, expected_output}] (3 cases)
                    - constraints: time/space complexity hints
                    """;
            case "CALCULATION", "MATH" -> """
                    - problem_statement: the calculation or math problem
                    - starter_code: formula template with blanks (use \\n for newlines)
                    - test_cases: [{input, expected_output}] (2 cases)
                    - constraints: which formula or method to use
                    """;
            default -> """
                    - problem_statement: a question or scenario to answer
                    - starter_code: response outline (use \\n for newlines)
                    - test_cases: [{criteria, expected_quality}] (2 items)
                    - constraints: key points that must be covered
                    """;
        };

        String systemPrompt = """
                You are an educator. Generate ONE practice problem for the given concept.

                Style: %s
                Format: %s
                Difficulty: %s
                Problem type: %s

                STRICT JSON RULES:
                - Return ONLY valid JSON, NO markdown
                - ALL values must be single-line strings
                - Use \\n for line breaks inside strings
                - No raw newlines inside string values
                - Parsable by standard JSON parser

                {
                  "problem_statement": "...",
                  "problem_type": "%s",
                  "language": "text",
                  "starter_code": "...",
                  "test_cases": [{"input":"...","expected_output":"..."}],
                  "constraints": "...",
                  "hints": ["hint1","hint2","hint3"]
                }
                """.formatted(styleGuide, typeGuide, difficulty, problemType, problemType);

        String raw = callWithRetry(systemPrompt,
                "Topic: %s. Concept: %s. Difficulty: %s.".formatted(topicGoal, conceptName, difficulty),
                2_000, 0.7);
        return parseProblemResponse(raw, conceptName, difficulty, problemType);
    }

    // ─── Evaluate submission ──────────────────────────────────────────────
    // FIX Issue 3: sanitizeForPrompt() prevents CTRL-CHAR JSON parse error
    // Multiline user code with raw \n was breaking Jackson on the server

    public EvaluationResult evaluateSubmission(String problemStatement,
            String userSubmission, String problemType, String language, String conceptName) {

        // FIX: sanitize both inputs — removes raw control characters from multiline
        // code
        String safeSub = sanitizeForPrompt(userSubmission);
        String safeProblem = sanitizeForPrompt(problemStatement);

        String guide = switch (problemType) {
            case "CODING" -> "Correctness, time complexity, edge cases, code quality.";
            case "CALCULATION", "MATH" -> "Correct formula, steps shown, final answer correct, units.";
            default -> "Addresses core question, factual accuracy, clarity, depth.";
        };

        String systemPrompt = """
                You are an expert evaluator. Evaluate the student's submission.
                Criteria: %s
                Score 0-10 (10=perfect, 6-7=good, below 6=needs work).
                Respond ONLY with valid JSON. No markdown.
                {
                  "score": 7,
                  "passed": true,
                  "strengths": "...",
                  "issues": "...",
                  "suggestions": "...",
                  "corrected_solution": "...",
                  "line_feedback": [{"line":"...","comment":"..."}]
                }
                """.formatted(guide);

        String userMessage = "Problem: %s\n\nStudent's %s submission:\n%s\n\nConcept: %s"
                .formatted(safeProblem, language, safeSub, conceptName);

        return parseEvaluationResult(callWithRetry(systemPrompt, userMessage, 2_000, 0.5));
    }

    // ─── FIX Issue 2: proper problem type inference ───────────────────────
    // Finance, Accounting, Investment → CALCULATION (not CODING or WRITTEN)
    // Only pure software engineering topics → CODING

    private String inferProblemType(String topicGoal, String conceptName) {
        String combined = ((topicGoal != null ? topicGoal : "") + " " +
                (conceptName != null ? conceptName : "")).toLowerCase();

        boolean isCoding = combined.matches(".*(dsa|algorithm|data structure|" +
                "programming|system design|spring|kotlin|javascript|typescript|" +
                "backend|frontend|leetcode|coding|software).*");

        boolean isCalc = combined.matches(".*(math|calcul|statistic|physics|" +
                "linear algebra|probabilit|interest|financ|account|invest|" +
                "quantitat|formula|ratio|return|yield|budget|econom|" +
                "chemist|biology|engineer|circuit).*");

        if (isCoding)
            return "CODING";
        if (isCalc)
            return "CALCULATION";
        return "WRITTEN";
    }

    // ─── FIX Issue 3: sanitize user input for embedding in prompts ────────
    // Prevents CTRL-CHAR (code 10 = \n) error in Jackson JSON parsing

    private String sanitizeForPrompt(String input) {
        if (input == null)
            return "";
        return input
                .replace("\\", "\\\\") // escape backslashes first
                .replace("\r\n", " ") // Windows newlines → space
                .replace("\n", " ") // Unix newlines → space
                .replace("\r", " ") // old Mac newlines → space
                .replace("\t", "  ") // tabs → spaces
                .replace("\"", "'") // double quotes → single (safe in JSON string)
                .replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", ""); // other control chars
    }

    // ─── Parse helpers ────────────────────────────────────────────────────

    private List<QuizQuestionResponse> parseQuizResponse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(cleanJson(raw));
            List<QuizQuestionResponse> q = new ArrayList<>();
            for (JsonNode n : root.path("questions")) {
                List<String> opts = new ArrayList<>();
                n.path("options").forEach(o -> opts.add(o.asText()));
                q.add(QuizQuestionResponse.builder()
                        .questionNumber(n.path("questionNumber").asInt())
                        .question(n.path("question").asText())
                        .options(opts)
                        .correctAnswerIndex(n.path("correctAnswerIndex").asInt())
                        .explanation(n.path("explanation").asText())
                        .build());
            }
            return q;
        } catch (Exception e) {
            log.error("Failed to parse quiz JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to parse quiz response");
        }
    }

    private List<String> parseTopicsResponse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(cleanJson(raw));
            List<String> topics = new ArrayList<>();
            root.path("topics").forEach(t -> topics.add(t.asText()));
            return topics;
        } catch (Exception e) {
            log.error("Failed to parse topics JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to parse roadmap response");
        }
    }

    private List<QuizSession.QuizQuestionData> parseConceptQuizResponse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(cleanJson(raw));
            List<QuizSession.QuizQuestionData> qs = new ArrayList<>();
            for (JsonNode q : root.path("questions")) {
                List<String> opts = new ArrayList<>();
                q.path("options").forEach(o -> opts.add(o.asText()));
                List<String> hints = new ArrayList<>();
                q.path("hints").forEach(h -> hints.add(h.asText()));
                qs.add(QuizSession.QuizQuestionData.builder()
                        .questionNumber(q.path("questionNumber").asInt())
                        .question(q.path("question").asText())
                        .options(opts).correctAnswerIndex(q.path("correctAnswerIndex").asInt())
                        .explanation(q.path("explanation").asText()).hints(hints).build());
            }
            return qs;
        } catch (Exception e) {
            log.error("Failed to parse concept quiz JSON: {}", e.getMessage());
            throw new RuntimeException("Failed to parse quiz response");
        }
    }

    private ProblemResponse parseProblemResponse(String raw, String conceptName,
            String difficulty, String problemType) {
        try {
            JsonNode root = objectMapper.readTree(cleanJson(raw));
            List<ProblemResponse.TestCase> tc = new ArrayList<>();
            for (JsonNode t : root.path("test_cases"))
                tc.add(ProblemResponse.TestCase.builder()
                        .input(t.path("input").asText())
                        .expectedOutput(t.path("expected_output").asText(t.path("expected_quality").asText()))
                        .build());
            List<String> hints = new ArrayList<>();
            root.path("hints").forEach(h -> hints.add(h.asText()));
            return ProblemResponse.builder().conceptName(conceptName).difficulty(difficulty)
                    .problemType(root.path("problem_type").asText(problemType))
                    .language(root.path("language").asText("text"))
                    .problemStatement(root.path("problem_statement").asText())
                    .starterCode(root.path("starter_code").asText())
                    .testCases(tc).constraints(root.path("constraints").asText()).hints(hints).build();
        } catch (Exception e) {
            log.error("Failed to parse problem: {}", e.getMessage());
            throw new RuntimeException("Failed to parse problem from AI");
        }
    }

    private EvaluationResult parseEvaluationResult(String raw) {
        try {
            JsonNode root = objectMapper.readTree(cleanJson(raw));
            List<EvaluationResult.LineFeedback> lf = new ArrayList<>();
            for (JsonNode l : root.path("line_feedback"))
                lf.add(EvaluationResult.LineFeedback.builder()
                        .line(l.path("line").asText()).comment(l.path("comment").asText()).build());
            return EvaluationResult.builder().score(root.path("score").asInt())
                    .passed(root.path("passed").asBoolean())
                    .strengths(root.path("strengths").asText()).issues(root.path("issues").asText())
                    .suggestions(root.path("suggestions").asText())
                    .correctedSolution(root.path("corrected_solution").asText())
                    .lineFeedback(lf).build();
        } catch (Exception e) {
            log.error("Failed to parse evaluation: {}", e.getMessage());
            throw new RuntimeException("Failed to parse evaluation from AI");
        }
    }

    private String cleanJson(String raw) {
        return raw.replaceAll("(?s)```json\\s*", "").replaceAll("(?s)```\\s*", "").trim();
    }

    private List<Map<String, Object>> buildMessagesWithSystem(
            String systemPrompt, List<Map<String, Object>> history) {
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