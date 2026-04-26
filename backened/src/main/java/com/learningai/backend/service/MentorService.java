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

                // Build messages list for Groq
                // [system already separate] + last N history + new user message
                String groqResponse = callGroqWithHistory(
                                systemPrompt, history, englishMessage);

                String finalResponse = langCtx.wasEnglish()
                                ? groqResponse
                                : languageService.translateFromEnglish(
                                                groqResponse,
                                                langCtx.languageCode(),
                                                langCtx.languageName());

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
                addToRedisContext(userId, userMsg);
                addToRedisContext(userId, assistantMsg);

                // Update DB session
                List<MentorSession.MentorMessage> allMessages = session.getMessages() != null
                                ? new ArrayList<>(session.getMessages())
                                : new ArrayList<>();
                allMessages.add(userMsg);
                allMessages.add(assistantMsg);

                session.setMessages(allMessages);
                session.setLastMessageAt(Instant.now());
                sessionRepository.save(session);

                log.info("Mentor chat — user:{} personality:{} messages:{}",
                                userId, personality, allMessages.size());

                return MentorChatResponse.builder()
                                .sessionId(session.getId())
                                .reply(groqResponse)
                                .personality(personality)
                                .messageCount(allMessages.size())
                                .currentDifficulty(profile != null
                                                ? profile.getCurrentDifficulty()
                                                : "MEDIUM")
                                .learningStyle(profile != null
                                                ? profile.getLearningStyle()
                                                : "PRACTICE")
                                .timestamp(Instant.now())
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
                                                .messageCount(s.getMessages() != null
                                                                ? s.getMessages().size()
                                                                : 0)
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

                // Close current active session
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

                // Personality-based tone
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
                        default -> // ENCOURAGING
                                """
                                                You are warm, encouraging, and patient.
                                                Celebrate small wins. When the student is wrong,
                                                say "Good try! Let's think about it differently..."
                                                Build confidence while building knowledge.
                                                """;
                };

                // DNA context injection
                String dnaContext = "";
                if (profile != null) {
                        String weakList = profile.getWeakConcepts().isEmpty()
                                        ? "none identified yet"
                                        : String.join(", ",
                                                        profile.getWeakConcepts().keySet()
                                                                        .stream().limit(3)
                                                                        .collect(Collectors.toList()));

                        String strongList = profile.getStrongConcepts().isEmpty()
                                        ? "none identified yet"
                                        : String.join(", ",
                                                        profile.getStrongConcepts().keySet()
                                                                        .stream().limit(3)
                                                                        .collect(Collectors.toList()));

                        // Get current topic from roadmap
                        String currentTopic = "not started";
                        List<String> roadmap = profile.getRoadmapTopics();
                        int idx = profile.getCurrentTopicIndex();
                        if (roadmap != null && !roadmap.isEmpty()
                                        && idx < roadmap.size()) {
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
                                You are Aria, an expert AI learning mentor...

                                CONVERSATION RULES:
                                - Keep responses focused and concise (2-4 paragraphs max)
                                - Always end with ONE question to keep the student engaged
                                - Remember what was discussed earlier in this conversation
                                - If student seems confused, slow down and simplify
                                - Never say "As an AI" — you ARE Aria, a real mentor
                                - Do not repeat the student's question back to them
                                - Always respond in English. Translation is handled separately.\n"
                                %s
                                """,
                                personalityGuide + dnaContext);
        }

        // ─── Call Groq with conversation history ─────────────────────────────

        private String callGroqWithHistory(String systemPrompt,
                        List<MentorSession.MentorMessage> history,
                        String newUserMessage) {
                try {
                        // Build messages array for Groq
                        // Format: [{role, content}, ...history..., {role: user, content: new}]
                        List<Map<String, Object>> messages = new ArrayList<>();

                        // Add conversation history (last N messages)
                        List<MentorSession.MentorMessage> recentHistory = history.size() > CONTEXT_WINDOW
                                        ? history.subList(
                                                        history.size() - CONTEXT_WINDOW,
                                                        history.size())
                                        : history;

                        for (MentorSession.MentorMessage msg : recentHistory) {
                                messages.add(Map.of(
                                                "role", msg.getRole(),
                                                "content", msg.getContent()));
                        }

                        

                        String finalUserMessage = """
                                        USER MESSAGE:
                                        %s

                                        IMPORTANT:
                                        - Ignore the language used in previous messages
                                        - Follow ONLY the language of THIS message
                                        """.formatted(newUserMessage);

                        // Add new user message
                        messages.add(Map.of(
                                        "role", "user",
                                        "content", finalUserMessage));

                        // Call Groq with full history
                        return aiService.callWithHistory(systemPrompt, messages);

                } catch (Exception e) {
                        log.error("Mentor Groq call failed: {}", e.getMessage());
                        return "I'm having trouble connecting right now. " +
                                        "Please try again in a moment!";
                }
        }

        // ─── Redis context management ─────────────────────────────────────────

        @SuppressWarnings("unchecked")
        private List<MentorSession.MentorMessage> getContextFromRedis(
                        UUID userId) {
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

        private void addToRedisContext(UUID userId,
                        MentorSession.MentorMessage message) {
                try {
                        String key = REDIS_KEY + userId;
                        List<MentorSession.MentorMessage> context = getContextFromRedis(userId);

                        context.add(message);

                        // Keep only last MAX_MESSAGES
                        if (context.size() > MAX_MESSAGES) {
                                context = context.subList(
                                                context.size() - MAX_MESSAGES,
                                                context.size());
                        }

                        redisTemplate.opsForValue().set(key, context, CONTEXT_TTL);
                } catch (Exception e) {
                        log.warn("Failed to update Redis context: {}", e.getMessage());
                }
        }

        // ─── Session management ───────────────────────────────────────────────

        private MentorSession getOrCreateSession(User user,
                        LearningProfile profile) {
                return sessionRepository
                                .findTopByUserIdAndStatusOrderByCreatedAtDesc(
                                                user.getId(), MentorSession.SessionStatus.ACTIVE)
                                .orElseGet(() -> createNewSession(user, profile));
        }

        private MentorSession createNewSession(User user,
                        LearningProfile profile) {
                MentorSession session = MentorSession.builder()
                                .user(user)
                                .messages(new ArrayList<>())
                                .status(MentorSession.SessionStatus.ACTIVE)
                                .snapshotDifficulty(profile != null
                                                ? profile.getCurrentDifficulty()
                                                : "MEDIUM")
                                .snapshotLearningStyle(profile != null
                                                ? profile.getLearningStyle()
                                                : "PRACTICE")
                                .snapshotAccuracy(profile != null
                                                ? profile.getOverallAccuracy()
                                                : 0.0)
                                .sessionTopic(profile != null
                                                ? profile.getGoal()
                                                : "General")
                                .build();
                return sessionRepository.save(session);
        }

        // ─── Helpers ──────────────────────────────────────────────────────────

        private String resolvePersonality(String override,
                        LearningProfile profile) {
                if (override != null && !override.isBlank()) {
                        return override.toUpperCase();
                }
                // Default personality based on learning style
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
}