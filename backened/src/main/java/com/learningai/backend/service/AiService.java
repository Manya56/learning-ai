package com.learningai.backend.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningai.backend.config.GroqKeyPool;
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

    private final WebClient    groqWebClient;
    private final ObjectMapper objectMapper;
    private final GroqKeyPool  keyPool;

    @Value("${groq.model}")
    private String model;

    private static final int  MAX_RETRIES      = 3;
    private static final long RETRY_BACKOFF_MS = 500;
    private static final int  TIMEOUT_SECONDS  = 30;

    public String call(String systemPrompt, String userMessage) {
        return callWithRetry(systemPrompt, userMessage, 2_000, 0.7);
    }

    public String callWithHistory(String systemPrompt, List<Map<String, Object>> messages) {
        return callHistoryWithRetry(systemPrompt, messages, 2_000, 0.8);
    }

    private String callWithRetry(String systemPrompt, String userMessage, int maxTokens, double temperature) {
        int attempts = Math.max(MAX_RETRIES, keyPool.poolSize());
        for (int attempt = 1; attempt <= attempts; attempt++) {
            String currentKey = keyPool.nextKey();
            try {
                Map<String, Object> body = Map.of("model", model, "max_tokens", maxTokens,
                        "temperature", temperature, "messages", List.of(
                                Map.of("role", "system", "content", systemPrompt),
                                Map.of("role", "user", "content", userMessage)));
                JsonNode response = groqWebClient.post()
                        .uri("/openai/v1/chat/completions")
                        .header("Authorization", "Bearer " + currentKey)
                        .bodyValue(body).retrieve().bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS)).block();
                return extractContent(response);
            } catch (WebClientResponseException.TooManyRequests e) {
                keyPool.markRateLimited(currentKey);
                log.warn("429 attempt {}/{} rotating key", attempt, attempts);
            } catch (WebClientResponseException e) {
                keyPool.markError(currentKey, e.getStatusCode().value());
                if (attempt == attempts) throw new RuntimeException("Groq error: " + e.getMessage());
                sleep(RETRY_BACKOFF_MS);
            } catch (Exception e) {
                if (attempt == attempts) throw new RuntimeException("Groq unavailable: " + e.getMessage());
                sleep(RETRY_BACKOFF_MS);
            }
        }
        throw new RuntimeException("All Groq keys exhausted");
    }

    private String callHistoryWithRetry(String systemPrompt, List<Map<String, Object>> messages, int maxTokens, double temperature) {
        List<Map<String, Object>> full = buildMessagesWithSystem(systemPrompt, messages);
        int attempts = Math.max(MAX_RETRIES, keyPool.poolSize());
        for (int attempt = 1; attempt <= attempts; attempt++) {
            String currentKey = keyPool.nextKey();
            try {
                Map<String, Object> body = Map.of("model", model, "max_tokens", maxTokens,
                        "temperature", temperature, "messages", full);
                JsonNode response = groqWebClient.post()
                        .uri("/openai/v1/chat/completions")
                        .header("Authorization", "Bearer " + currentKey)
                        .bodyValue(body).retrieve().bodyToMono(JsonNode.class)
                        .timeout(Duration.ofSeconds(TIMEOUT_SECONDS)).block();
                return extractContent(response);
            } catch (WebClientResponseException.TooManyRequests e) {
                keyPool.markRateLimited(currentKey);
            } catch (WebClientResponseException e) {
                if (attempt == attempts) throw new RuntimeException("Groq error: " + e.getMessage());
                sleep(RETRY_BACKOFF_MS);
            } catch (Exception e) {
                if (attempt == attempts) throw new RuntimeException("Groq unavailable: " + e.getMessage());
                sleep(RETRY_BACKOFF_MS);
            }
        }
        throw new RuntimeException("All Groq keys exhausted on history call");
    }

    private String extractContent(JsonNode response) {
        if (response == null) throw new RuntimeException("Groq returned null");
        JsonNode choices = response.path("choices");
        if (choices.isEmpty()) throw new RuntimeException("Groq empty choices: " + response);
        String content = choices.get(0).path("message").path("content").asText();
        if (content == null || content.isBlank()) throw new RuntimeException("Groq empty content");
        return content;
    }

    public List<QuizQuestionResponse> generateKnowledgeQuiz(String goal, int priorLevel) {
        String sp = "You are an expert educator. Generate exactly 5 MCQ questions for the topic.\nWorks for ANY domain.\nACCURACY: verify each answer.\nRespond ONLY with valid JSON:\n{\"questions\":[{\"questionNumber\":1,\"question\":\"...\",\"options\":[\"A\",\"B\",\"C\",\"D\"],\"correctAnswerIndex\":0,\"explanation\":\"...\"}]}";
        return parseQuizResponse(callWithRetry(sp, "Topic: %s. Level: %d/3.".formatted(goal, priorLevel), 2_000, 0.3));
    }

    public List<String> generateRoadmapTopics(String goal, String difficulty, String desc) {
        String sp = "You are a curriculum designer. Generate 10 ordered topics for ANY domain.\nRespond ONLY with valid JSON: {\"topics\":[\"Topic 1\",\"Topic 2\"]}";
        return parseTopicsResponse(callWithRetry(sp, "Goal: %s. Desc: %s. Difficulty: %s.".formatted(goal, desc, difficulty), 1_000, 0.7));
    }

    public List<QuizSession.QuizQuestionData> generateQuizForConcept(String conceptName, String topicGoal, String difficulty, String learningStyle, int count) {
        String domain = (topicGoal != null && !topicGoal.isBlank()) ? topicGoal : "the given subject";
        String style = switch (learningStyle) {
            case "PRACTICE" -> "Focus on application.";
            case "READING"  -> "Test conceptual understanding.";
            case "VISUAL"   -> "Use scenario-based questions.";
            default         -> "Mix conceptual and application.";
        };
        String sp = """
You are an expert in: %s. Generate quiz questions about a concept WITHIN this domain.
ACCURACY: verify each answer. Only include questions you are 100%% certain about.
Generate exactly %d questions. Difficulty: %s. Style: %s
Respond ONLY with valid JSON:
{"questions":[{"questionNumber":1,"question":"...","options":["A","B","C","D"],"correctAnswerIndex":0,"explanation":"...","hints":["h1","h2","h3"]}]}
""".formatted(domain, count, difficulty, style);
        String um = "Domain: %s\nConcept: %s\nDifficulty: %s\nGenerate %d questions about '%s' in context of %s.".formatted(domain, conceptName, difficulty, count, conceptName, domain);
        return parseConceptQuizResponse(callWithRetry(sp, um, 3_000, 0.3));
    }

    public String generateHint(String question, String conceptName, int hintNumber) {
        String sp = "Give hint #%d. 1=nudge, 2=approach, 3=near-answer. Under 2 sentences. Only hint text.".formatted(hintNumber);
        return callWithRetry(sp, "Q: " + question + "\nConcept: " + conceptName, 300, 0.6).trim();
    }

    public ProblemResponse generateProblem(String conceptName, String topicGoal, String difficulty, String learningStyle) {
        String problemType = inferProblemType(topicGoal, conceptName);
        String styleGuide = switch (learningStyle) {
            case "PRACTICE" -> "Direct hands-on."; case "READING" -> "Include context."; case "VISUAL" -> "Use real-world scenario."; default -> "Clear, direct.";
        };
        String sp = "You are an educator in: %s. Generate ONE practice problem within \"%s\".\nStyle: %s  Type: %s  Difficulty: %s\nSTRICT JSON, no markdown, all values single-line:\n{\"problem_statement\":\"...\",\"problem_type\":\"%s\",\"language\":\"text\",\"starter_code\":\"...\",\"test_cases\":[{\"input\":\"...\",\"expected_output\":\"...\"}],\"constraints\":\"...\",\"hints\":[\"h1\",\"h2\",\"h3\"]}".formatted(topicGoal, topicGoal, styleGuide, problemType, difficulty, problemType);
        return parseProblemResponse(callWithRetry(sp, "Domain: %s. Concept: %s. Difficulty: %s.".formatted(topicGoal, conceptName, difficulty), 2_000, 0.7), conceptName, difficulty, problemType);
    }

    public EvaluationResult evaluateSubmission(String problemStatement, String userSubmission, String problemType, String language, String conceptName) {
        String guide = switch (problemType) { case "CODING" -> "Correctness, complexity, edge cases, quality."; case "CALCULATION","MATH" -> "Formula, steps, answer, units."; default -> "Addresses question, accuracy, clarity, depth."; };
        String sp = "You are an expert evaluator. Criteria: %s. Score 0-10.\nRespond ONLY valid JSON:\n{\"score\":7,\"passed\":true,\"strengths\":\"...\",\"issues\":\"...\",\"suggestions\":\"...\",\"corrected_solution\":\"...\",\"line_feedback\":[{\"line\":\"...\",\"comment\":\"...\"}]}".formatted(guide);
        return parseEvaluationResult(callWithRetry(sp, "Problem: %s\n\nStudent %s submission:\n%s\n\nConcept: %s".formatted(sanitizeForPrompt(problemStatement), language, sanitizeForPrompt(userSubmission), conceptName), 2_000, 0.5));
    }

    public String generateMotivationalMessage(String goal, double accuracy, int streak, List<String> weakConcepts) {
        String sp = "You are an encouraging learning coach. Write ONE short motivational message (max 120 chars) for a push notification. Be specific. Only the message text.";
        String um = "Goal: %s\nAccuracy: %.0f%%\nStreak: %d days\nWeak areas: %s\nWrite 1-sentence motivation.".formatted(goal, accuracy * 100, streak, weakConcepts.isEmpty() ? "none" : String.join(", ", weakConcepts));
        try { return callWithRetry(sp, um, 150, 0.9).trim(); }
        catch (Exception e) { return "Keep going! Every concept gets you closer to your goal. 🚀"; }
    }

    private String inferProblemType(String topicGoal, String conceptName) {
        String c = ((topicGoal != null ? topicGoal : "") + " " + (conceptName != null ? conceptName : "")).toLowerCase();
        if (c.matches(".*(dsa|algorithm|data structure|programming|system design|spring|kotlin|javascript|typescript|backend|frontend|leetcode|coding|software).*")) return "CODING";
        if (c.matches(".*(math|calcul|statistic|physics|linear algebra|probabilit|interest|financ|account|invest|quantitat|formula|ratio|return|yield|budget|econom|chemist|biology|engineer|circuit).*")) return "CALCULATION";
        return "WRITTEN";
    }

    private String sanitizeForPrompt(String input) {
        if (input == null) return "";
        return input.replace("\\","\\\\").replace("\r\n"," ").replace("\n"," ").replace("\r"," ").replace("\t","  ").replace("\"","'").replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]","");
    }

    private List<QuizQuestionResponse> parseQuizResponse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(cleanJson(raw)); List<QuizQuestionResponse> q = new ArrayList<>();
            for (JsonNode n : root.path("questions")) { List<String> opts = new ArrayList<>(); n.path("options").forEach(o -> opts.add(o.asText())); q.add(QuizQuestionResponse.builder().questionNumber(n.path("questionNumber").asInt()).question(n.path("question").asText()).options(opts).correctAnswerIndex(n.path("correctAnswerIndex").asInt()).explanation(n.path("explanation").asText()).build()); }
            return q;
        } catch (Exception e) { throw new RuntimeException("Failed to parse quiz: " + e.getMessage()); }
    }

    private List<String> parseTopicsResponse(String raw) {
        try { JsonNode root = objectMapper.readTree(cleanJson(raw)); List<String> t = new ArrayList<>(); root.path("topics").forEach(n -> t.add(n.asText())); return t; }
        catch (Exception e) { throw new RuntimeException("Failed to parse topics: " + e.getMessage()); }
    }

    private List<QuizSession.QuizQuestionData> parseConceptQuizResponse(String raw) {
        try {
            JsonNode root = objectMapper.readTree(cleanJson(raw)); List<QuizSession.QuizQuestionData> qs = new ArrayList<>();
            for (JsonNode q : root.path("questions")) { List<String> opts = new ArrayList<>(); q.path("options").forEach(o -> opts.add(o.asText())); List<String> hints = new ArrayList<>(); q.path("hints").forEach(h -> hints.add(h.asText())); qs.add(QuizSession.QuizQuestionData.builder().questionNumber(q.path("questionNumber").asInt()).question(q.path("question").asText()).options(opts).correctAnswerIndex(q.path("correctAnswerIndex").asInt()).explanation(q.path("explanation").asText()).hints(hints).build()); }
            return qs;
        } catch (Exception e) { throw new RuntimeException("Failed to parse concept quiz: " + e.getMessage()); }
    }

    private ProblemResponse parseProblemResponse(String raw, String conceptName, String difficulty, String problemType) {
        try {
            JsonNode root = objectMapper.readTree(cleanJson(raw)); List<ProblemResponse.TestCase> tc = new ArrayList<>();
            for (JsonNode t : root.path("test_cases")) tc.add(ProblemResponse.TestCase.builder().input(t.path("input").asText()).expectedOutput(t.path("expected_output").asText(t.path("expected_quality").asText())).build());
            List<String> hints = new ArrayList<>(); root.path("hints").forEach(h -> hints.add(h.asText()));
            return ProblemResponse.builder().conceptName(conceptName).difficulty(difficulty).problemType(root.path("problem_type").asText(problemType)).language(root.path("language").asText("text")).problemStatement(root.path("problem_statement").asText()).starterCode(root.path("starter_code").asText()).testCases(tc).constraints(root.path("constraints").asText()).hints(hints).build();
        } catch (Exception e) { throw new RuntimeException("Failed to parse problem: " + e.getMessage()); }
    }

    private EvaluationResult parseEvaluationResult(String raw) {
        try {
            JsonNode root = objectMapper.readTree(cleanJson(raw)); List<EvaluationResult.LineFeedback> lf = new ArrayList<>();
            for (JsonNode l : root.path("line_feedback")) lf.add(EvaluationResult.LineFeedback.builder().line(l.path("line").asText()).comment(l.path("comment").asText()).build());
            return EvaluationResult.builder().score(root.path("score").asInt()).passed(root.path("passed").asBoolean()).strengths(root.path("strengths").asText()).issues(root.path("issues").asText()).suggestions(root.path("suggestions").asText()).correctedSolution(root.path("corrected_solution").asText()).lineFeedback(lf).build();
        } catch (Exception e) { throw new RuntimeException("Failed to parse evaluation: " + e.getMessage()); }
    }

    private String cleanJson(String raw) { return raw.replaceAll("(?s)```json\\s*","").replaceAll("(?s)```\\s*","").trim(); }

    private List<Map<String, Object>> buildMessagesWithSystem(String sp, List<Map<String, Object>> history) {
        List<Map<String, Object>> all = new ArrayList<>(); all.add(Map.of("role","system","content",sp)); all.addAll(history); return all;
    }

    private void sleep(long ms) { try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); } }
}