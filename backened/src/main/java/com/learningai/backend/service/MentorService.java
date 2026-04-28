package com.learningai.backend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.learningai.backend.dto.response.MentorChatResponse;
import com.learningai.backend.dto.response.MentorHistoryResponse;
import com.learningai.backend.entity.LearningProfile;
import com.learningai.backend.entity.MentorSession;
import com.learningai.backend.entity.User;
import com.learningai.backend.exception.AppException;
import com.learningai.backend.repository.LearningProfileRepository;
import com.learningai.backend.repository.MentorSessionRepository;
import com.learningai.backend.repository.UserRepository;
import com.learningai.backend.service.scraper.ContentPipelineService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class MentorService {

        private final AiService aiService;
        private final MentorSessionRepository sessionRepository;
        private final LearningProfileRepository profileRepository;
        private final UserRepository userRepository;
        private final RedisTemplate<String, Object> redisTemplate;
        private final ObjectMapper objectMapper;
        private final LanguageService languageService;
        private final ContentPipelineService pipelineService;

        // Redis key prefix for conversation context
        private static final String REDIS_KEY = "mentor:context:";
        // Max messages kept in Redis per user
        private static final int MAX_MESSAGES = 20;
        // TTL for Redis conversation context
        private static final Duration CONTEXT_TTL = Duration.ofDays(7);
        // Max messages sent to Groq (keep prompt manageable)
        private static final int CONTEXT_WINDOW = 10;

        // ─── Main chat method ─────────────────────────────────────────────────

        @Transactional
        public MentorChatResponse chat(UUID userId,
                        String userMessage,
                        String personalityOverride,
                        boolean newSession) {

                User user = getUser(userId);
                LearningProfile profile = profileRepository
                                .findByUserId(userId).orElse(null);

                LanguageService.TranslationContext langCtx = languageService.prepareQuery(userMessage);

                String englishMessage = langCtx.englishQuery();

                // Get or create session
                MentorSession session = newSession
                                ? createNewSession(user, profile)
                                : getOrCreateSession(user, profile);

                // Determine personality
                String personality = resolvePersonality(
                                personalityOverride, profile);

                // Build conversation history from Redis
                List<MentorSession.MentorMessage> history = getContextFromRedis(userId);

                // Build system prompt with DNA injection
                String systemPrompt = buildSystemPrompt(
                                personality, profile, englishMessage);

                // ── Call Groq — with graceful fallback ────────────────────────────
                String groqResponse;
                try {
                        groqResponse = callGroqWithHistory(systemPrompt, history, englishMessage);
                } catch (Exception e) {
                        log.error("Mentor Groq call failed for user {}: {}", userId, e.getMessage());
                        groqResponse = buildFallbackResponse(personality);
                }

                // ── Cache knowledge if it's a learning question ───────────────────
                if (isKnowledgeQuestion(englishMessage) && profile != null) {
                        try {
                                pipelineService.storeAiKnowledge(
                                                englishMessage,
                                                groqResponse,
                                                "Mentor: " + userMessage.substring(0,
                                                                Math.min(50, userMessage.length())),
                                                profile.getGoal());
                        } catch (Exception e) {
                                log.warn("Failed to cache mentor knowledge: {}", e.getMessage());
                        }
                }

                String finalResponse = langCtx.wasEnglish()
                                ? groqResponse
                                : translateSafely(groqResponse, langCtx.languageCode(), langCtx.languageName());

                Instant now = Instant.now();

                // Save user message + AI response to Redis
                MentorSession.MentorMessage userMsg = MentorSession.MentorMessage.builder()
                                .role("user")
                                .content(userMessage)
                                .timestamp(Instant.now())
                                .build();

                MentorSession.MentorMessage assistantMsg = MentorSession.MentorMessage.builder()
                                .role("assistant")
                                .content(finalResponse)
                                .timestamp(Instant.now())
                                .build();

                // Update Redis context
                // addToRedisContext(userId, userMsg);
                // addToRedisContext(userId, assistantMsg);
                saveToRedisContext(userId, userMsg, assistantMsg);

                // Update DB session
                List<MentorSession.MentorMessage> allMessages = session.getMessages() != null
                                ? new ArrayList<>(session.getMessages())
                                : new ArrayList<>();
                allMessages.add(userMsg);
                allMessages.add(assistantMsg);

                session.setMessages(allMessages);
                session.setLastMessageAt(now);
                sessionRepository.save(session);

                log.info("Mentor chat — user:{} personality:{} messages:{}",
                                userId, personality, allMessages.size());

                return MentorChatResponse.builder()
                                .sessionId(session.getId())
                                .reply(finalResponse) // FIX: was groqResponse
                                .personality(personality)
                                .messageCount(allMessages.size())
                                .currentDifficulty(profile != null ? profile.getCurrentDifficulty() : "MEDIUM")
                                .learningStyle(profile != null ? profile.getLearningStyle() : "PRACTICE")
                                .timestamp(now)
                                .build();
        }

        // ─── Get chat history ─────────────────────────────────────────────────

        public List<MentorHistoryResponse> getHistory(UUID userId) {
                return sessionRepository
                                .findTop10ByUserIdOrderByCreatedAtDesc(userId)
                                .stream()
                                .map(s -> MentorHistoryResponse.builder()
                                                .sessionId(s.getId())
                                                .sessionTopic(s.getSessionTopic())
                                                .status(s.getStatus().name())
                                                .messageCount(s.getMessages() != null ? s.getMessages().size() : 0)
                                                .createdAt(s.getCreatedAt())
                                                .lastMessageAt(s.getLastMessageAt())
                                                .messages(s.getMessages())
                                                .build())
                                .collect(Collectors.toList());
        }
        // ─── Clear context (start fresh) ──────────────────────────────────────

        @Transactional
        public void clearContext(UUID userId) {
                String key = REDIS_KEY + userId;
                redisTemplate.delete(key);

                sessionRepository
                                .findTopByUserIdAndStatusOrderByCreatedAtDesc(
                                                userId, MentorSession.SessionStatus.ACTIVE)
                                .ifPresent(s -> {
                                        s.setStatus(MentorSession.SessionStatus.CLOSED);
                                        sessionRepository.save(s);
                                });

                log.info("Mentor context cleared for user: {}", userId);
        }

        // ─── Build DNA-aware system prompt ───────────────────────────────────

        private String buildSystemPrompt(String personality,
                        LearningProfile profile,
                        String userMessage) {

                String personalityGuide = switch (personality) {
                        case "STRICT" -> """
                                        You are strict and demanding. Push the student hard.
                                        Don't accept vague answers. Ask follow-up questions.
                                        Point out errors directly without sugar-coating.
                                        High standards lead to mastery.
                                        """;
                        case "SOCRATIC" -> """
                                        You teach only through questions. Never give direct answers.
                                        Guide the student to discover the answer themselves.
                                        Ask "What do you think happens when...?" instead of explaining.
                                        Only reveal the answer after the student has genuinely tried.
                                        """;
                        default -> // ENCOURAGING — also the safe default for unknown values
                                """
                                                You are warm, encouraging, and patient.
                                                Celebrate small wins. When the student is wrong,
                                                say "Good try! Let's think about it differently..."
                                                Build confidence while building knowledge.
                                                """;
                };

                // DNA context injection — null-safe throughout
                String dnaContext = "";
                if (profile != null) {
                        String weakList = (profile.getWeakConcepts() == null || profile.getWeakConcepts().isEmpty())
                                        ? "none identified yet"
                                        : profile.getWeakConcepts().keySet().stream().limit(3)
                                                        .collect(Collectors.joining(", "));

                        String strongList = (profile.getStrongConcepts() == null
                                        || profile.getStrongConcepts().isEmpty())
                                                        ? "none identified yet"
                                                        : profile.getStrongConcepts().keySet().stream().limit(3)
                                                                        .collect(Collectors.joining(", "));

                        String currentTopic = "not started";
                        List<String> roadmap = profile.getRoadmapTopics();
                        int idx = profile.getCurrentTopicIndex() != null ? profile.getCurrentTopicIndex() : 0;
                        if (roadmap != null && !roadmap.isEmpty() && idx < roadmap.size()) {
                                currentTopic = roadmap.get(idx);
                        }

                        dnaContext = String.format("""

                                        STUDENT LEARNING DNA:
                                        - Goal: %s
                                        - Current topic: %s
                                        - Difficulty level: %s
                                        - Learning style: %s
                                        - Overall accuracy: %.0f%%
                                        - Current streak: %d days
                                        - Weak concepts (needs help): %s
                                        - Strong concepts (knows well): %s

                                        Use this DNA to personalize every response.
                                        Reference their weak concepts when relevant.
                                        Don't repeat what they already know well.
                                        """,
                                        profile.getGoal(),
                                        currentTopic,
                                        profile.getCurrentDifficulty(),
                                        profile.getLearningStyle(),
                                        profile.getOverallAccuracy() * 100,
                                        profile.getCurrentDayStreak(),
                                        weakList,
                                        strongList);
                }

                return String.format("""
                                You are Aria, an expert AI learning mentor.

                                CONVERSATION RULES:
                                - Keep responses focused and concise (2-4 paragraphs max)
                                - Always end with ONE question to keep the student engaged
                                - Remember what was discussed earlier in this conversation
                                - If student seems confused, slow down and simplify
                                - Never say "As an AI" — you ARE Aria, a real mentor
                                - Do not repeat the student's question back to them
                                - Always respond in English. Translation is handled separately.
                                %s
                                """,
                                personalityGuide + dnaContext);
        }

        // ─── Call Groq with conversation history ─────────────────────────────

        private String callGroqWithHistory(String systemPrompt,
                        List<MentorSession.MentorMessage> history,
                        String newUserMessage) {

                List<Map<String, Object>> messages = new ArrayList<>();

                // Add last N messages from history
                List<MentorSession.MentorMessage> recentHistory = history.size() > CONTEXT_WINDOW
                                ? history.subList(history.size() - CONTEXT_WINDOW, history.size())
                                : history;

                for (MentorSession.MentorMessage msg : recentHistory) {
                        messages.add(Map.of(
                                        "role", msg.getRole(),
                                        "content", msg.getContent()));
                }

                // Add new user message with language instruction
                String formattedUserMessage = """
                                USER MESSAGE:
                                %s

                                IMPORTANT:
                                - Ignore the language used in previous messages
                                - Follow ONLY the language of THIS message
                                """.formatted(newUserMessage);

                messages.add(Map.of("role", "user", "content", formattedUserMessage));

                return aiService.callWithHistory(systemPrompt, messages);
        }

        // ─── Redis context management (FIX: batch read/write) ────────────────

        @SuppressWarnings("unchecked")
        private List<MentorSession.MentorMessage> getContextFromRedis(UUID userId) {
                try {
                        String key = REDIS_KEY + userId;
                        Object raw = redisTemplate.opsForValue().get(key);
                        if (raw == null)
                                return new ArrayList<>();
                        return objectMapper.convertValue(raw,
                                        new TypeReference<List<MentorSession.MentorMessage>>() {
                                        });
                } catch (Exception e) {
                        log.warn("Failed to get Redis context: {}", e.getMessage());
                        return new ArrayList<>();
                }
        }

        // private void addToRedisContext(UUID userId,
        // MentorSession.MentorMessage message) {
        // try {
        // String key = REDIS_KEY + userId;
        // List<MentorSession.MentorMessage> context = getContextFromRedis(userId);

        // context.add(message);

        // // Keep only last MAX_MESSAGES
        // if (context.size() > MAX_MESSAGES) {
        // context = context.subList(
        // context.size() - MAX_MESSAGES,
        // context.size());
        // }

        // redisTemplate.opsForValue().set(key, context, CONTEXT_TTL);
        // } catch (Exception e) {
        // log.warn("Failed to update Redis context: {}", e.getMessage());
        // }
        // }

        private void saveToRedisContext(UUID userId,
                        MentorSession.MentorMessage userMsg,
                        MentorSession.MentorMessage assistantMsg) {
                try {
                        String key = REDIS_KEY + userId;
                        List<MentorSession.MentorMessage> context = getContextFromRedis(userId);

                        context.add(userMsg);
                        context.add(assistantMsg);

                        // Keep only last MAX_MESSAGES
                        if (context.size() > MAX_MESSAGES) {
                                context = context.subList(context.size() - MAX_MESSAGES, context.size());
                        }

                        redisTemplate.opsForValue().set(key, context, CONTEXT_TTL);
                } catch (Exception e) {
                        log.warn("Failed to update Redis context: {}", e.getMessage());
                }
        }

        // ─── Session management ───────────────────────────────────────────────

        private MentorSession getOrCreateSession(User user, LearningProfile profile) {
                return sessionRepository
                                .findTopByUserIdAndStatusOrderByCreatedAtDesc(
                                                user.getId(), MentorSession.SessionStatus.ACTIVE)
                                .orElseGet(() -> createNewSession(user, profile));
        }

        private MentorSession createNewSession(User user, LearningProfile profile) {
                MentorSession session = MentorSession.builder()
                                .user(user)
                                .messages(new ArrayList<>())
                                .status(MentorSession.SessionStatus.ACTIVE)
                                .snapshotDifficulty(profile != null ? profile.getCurrentDifficulty() : "MEDIUM")
                                .snapshotLearningStyle(profile != null ? profile.getLearningStyle() : "PRACTICE")
                                .snapshotAccuracy(profile != null ? profile.getOverallAccuracy() : 0.0)
                                .sessionTopic(profile != null ? profile.getGoal() : "General")
                                .build();
                return sessionRepository.save(session);
        }

        private String buildFallbackResponse(String personality) {
                return switch (personality) {
                        case "STRICT" ->
                                "I'm experiencing connectivity issues. Review your last concept again and try later.";
                        case "SOCRATIC" ->
                                "What would you explore while I reconnect? Try thinking through the problem yourself!";
                        default -> "I'm having a moment! 😅 Give me a minute and try again — I'm here for you!";
                };
        }

        private String translateSafely(String text, String langCode, String langName) {
                try {
                        return languageService.translateFromEnglish(text, langCode, langName);
                } catch (Exception e) {
                        log.warn("Translation failed, returning English: {}", e.getMessage());
                        return text;
                }
        }

        // ─── Helpers ──────────────────────────────────────────────────────────

        private String resolvePersonality(String override, LearningProfile profile) {
                if (override != null && !override.isBlank()) {
                        return override.toUpperCase();
                }
                if (profile != null) {
                        return switch (profile.getLearningStyle()) {
                                case "READING" -> "SOCRATIC";
                                case "VISUAL" -> "ENCOURAGING";
                                case "PRACTICE" -> "STRICT";
                                default -> "ENCOURAGING";
                        };
                }
                return "ENCOURAGING";
        }

        private User getUser(UUID userId) {
                return userRepository.findById(userId)
                                .orElseThrow(() -> AppException.notFound("User not found"));
        }

        private boolean isKnowledgeQuestion(String message) {
                if (message == null)
                        return false;
                String lower = message.toLowerCase();
                return lower.contains("what is") ||
                                lower.contains("how does") ||
                                lower.contains("explain") ||
                                lower.contains("why is") ||
                                lower.contains("what are") ||
                                lower.contains("how to") ||
                                lower.contains("difference between") ||
                                lower.contains("kaise") || // Hindi "how"
                                lower.contains("kya hai") || // Hindi "what is"
                                lower.contains("batao") || // Hindi "tell me"
                                lower.contains("que es") || // Spanish "what is"
                                lower.contains("comment"); // French "how"
        }
}