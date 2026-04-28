package com.learningai.backend.service;

import com.learningai.backend.dto.response.*;
import com.learningai.backend.entity.*;
import com.learningai.backend.exception.AppException;
import com.learningai.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

        private final QuizSessionRepository sessionRepository;
        private final QuizAttemptRepository attemptRepository;
        private final LearningProfileRepository profileRepository;
        private final UserRepository userRepository;
        private final AiService aiService;
        private final LearningProfileService profileService;

        private final RedisTemplate<String, Object> redisTemplate;
        private final LanguageService languageService;

        private static final String HINT_KEY = "quiz:hints:";
        private static final int QUESTIONS_PER_SESSION = 5;
        private static final int HISTORY_PAGE_SIZE = 10;

        // ─── Start a new quiz session ─────────────────────────────────────────

        @Transactional
        public QuizSessionResponse startSession(UUID userId, String conceptName) {

                User user = getUser(userId);

                // Block if user already has an active session
                sessionRepository.findByUserIdAndStatus(
                                userId, QuizSession.SessionStatus.IN_PROGRESS)
                                .ifPresent(s -> {
                                        throw AppException.conflict(
                                                        "You have an active session. Complete it first. " +
                                                                        "Session ID: " + s.getId());
                                });

                // Fetch user's Learning DNA
                LearningProfile profile = profileRepository
                                .findByUserId(userId)
                                .orElseThrow(() -> AppException.notFound(
                                                "Complete onboarding before starting a quiz"));

                String difficulty = profile.getCurrentDifficulty();
                String learningStyle = profile.getLearningStyle();

                log.info("Generating quiz — user:{} concept:{} difficulty:{} style:{}",
                                userId, conceptName, difficulty, learningStyle);

                // Generate questions — wrap in try/catch so Groq failure gives a clean error
                List<QuizSession.QuizQuestionData> questions;
                try {
                        questions = aiService.generateQuizForConcept(
                                        conceptName, difficulty, learningStyle, QUESTIONS_PER_SESSION);
                } catch (Exception e) {
                        log.error("Quiz generation failed for concept {}: {}", conceptName, e.getMessage());
                        throw AppException.badRequest(
                                        "Could not generate quiz right now. Please try again in a moment.");
                }

                if (questions == null || questions.isEmpty()) {
                        throw AppException.badRequest(
                                        "AI returned empty quiz. Please try again.");
                }

                QuizSession session = QuizSession.builder()
                                .user(user)
                                .conceptName(conceptName)
                                .difficulty(difficulty)
                                .learningStyle(learningStyle)
                                .questions(questions)
                                .status(QuizSession.SessionStatus.IN_PROGRESS)
                                .totalQuestions(questions.size())
                                .build();

                session = sessionRepository.save(session);
                log.info("Quiz session created: {}", session.getId());

                // Return WITHOUT correct answer indices — answers stay server-side
                return mapToSessionResponse(session, false);
        }

        // ─── Submit a single answer ───────────────────────────────────────────

        @Transactional
        public AnswerFeedbackResponse submitAnswer(UUID userId,
                        UUID sessionId,
                        int questionIndex,
                        int selectedIndex,
                        long timeTakenMs) {

                // SECURITY: always load session from DB — never trust client for correct answer
                QuizSession session = getActiveSession(sessionId, userId);
                User user = session.getUser();

                LearningProfile profile = profileRepository.findByUserId(userId).orElse(null);

                // Validate bounds
                if (questionIndex < 0 || questionIndex >= session.getQuestions().size()) {
                        throw AppException.badRequest("Invalid question index");
                }

                // Check not already answered
                List<QuizAttempt> existing = attemptRepository
                                .findBySessionIdOrderByQuestionIndexAsc(sessionId);
                boolean alreadyAnswered = existing.stream()
                                .anyMatch(a -> a.getQuestionIndex() == questionIndex);
                if (alreadyAnswered) {
                        throw AppException.conflict("Question " + questionIndex + " already answered");
                }

                // Correct answer comes from DB (server-side) — NOT from client
                QuizSession.QuizQuestionData qData = session.getQuestions().get(questionIndex);
                boolean correct = (selectedIndex == qData.getCorrectAnswerIndex());
                int hintsUsed = countHintsUsed(sessionId, questionIndex);

                // Save the attempt
                QuizAttempt attempt = QuizAttempt.builder()
                                .session(session)
                                .user(user)
                                .questionIndex(questionIndex)
                                .selectedAnswerIndex(selectedIndex)
                                .correct(correct)
                                .timeTakenMs(timeTakenMs)
                                .hintsUsed(hintsUsed)
                                .conceptName(session.getConceptName())
                                .build();
                attemptRepository.save(attempt);

                // Update Learning DNA immediately
                profileService.recordAttempt(
                                userId,
                                session.getConceptName(),
                                correct,
                                timeTakenMs,
                                hintsUsed > 0,
                                false // not a coding question
                );

                log.info("Answer submitted — user:{} q:{} correct:{} time:{}ms",
                                userId, questionIndex, correct, timeTakenMs);

                // ── Translate explanation if needed ───────────────────────────────
                String explanation = qData.getExplanation();
                if (profile != null &&
                                !"English".equalsIgnoreCase(profile.getPreferredLanguage())) {
                        try {
                                LanguageService.DetectionResult det = languageService
                                                .detect(profile.getPreferredLanguage());
                                if (!"en".equals(det.languageCode())) {
                                        explanation = languageService.translateFromEnglish(
                                                        explanation,
                                                        det.languageCode(),
                                                        profile.getPreferredLanguage());
                                }
                        } catch (Exception e) {
                                log.warn("Explanation translation failed, using English: {}", e.getMessage());
                                // Keep English explanation — non-critical
                        }
                }

                return AnswerFeedbackResponse.builder()
                                .questionIndex(questionIndex)
                                .selectedIndex(selectedIndex)
                                .correctIndex(qData.getCorrectAnswerIndex())
                                .correct(correct)
                                .explanation(explanation)
                                .conceptName(session.getConceptName())
                                .build();
        }
        // ─── Get a hint for a question ────────────────────────────────────────

        public HintResponse getHint(UUID userId,
                        UUID sessionId,
                        int questionIndex,
                        int hintNumber) {

                QuizSession session = getActiveSession(sessionId, userId);

                if (questionIndex < 0 || questionIndex >= session.getQuestions().size()) {
                        throw AppException.badRequest("Invalid question index");
                }

                if (hintNumber < 1 || hintNumber > 3) {
                        throw AppException.badRequest("Hint number must be 1, 2, or 3");
                }

                QuizSession.QuizQuestionData qData = session.getQuestions().get(questionIndex);
                List<String> hints = qData.getHints();

                String hintText;
                // Use pre-generated hint if available, otherwise generate live
                if (hints != null && hints.size() >= hintNumber) {
                        hintText = hints.get(hintNumber - 1);
                } else {
                        // Fallback to live generation — with error handling
                        try {
                                hintText = aiService.generateHint(
                                                qData.getQuestion(),
                                                session.getConceptName(),
                                                hintNumber);
                        } catch (Exception e) {
                                log.warn("Live hint generation failed: {}", e.getMessage());
                                hintText = "Think carefully about the core concept of " +
                                                session.getConceptName() + " and what the question is testing.";
                        }
                }

                log.info("Hint {} given — user:{} session:{} q:{}",
                                hintNumber, userId, sessionId, questionIndex);

                trackHintUsed(sessionId, questionIndex);

                return HintResponse.builder()
                                .questionIndex(questionIndex)
                                .hintNumber(hintNumber)
                                .hint(hintText)
                                .build();
        }

        // ─── Complete a session ───────────────────────────────────────────────

        @Transactional
        public QuizResultResponse completeSession(UUID userId, UUID sessionId) {

                QuizSession session = getActiveSession(sessionId, userId);

                List<QuizAttempt> attempts = attemptRepository
                                .findBySessionIdOrderByQuestionIndexAsc(sessionId);

                int totalCorrect = (int) attempts.stream()
                                .filter(QuizAttempt::getCorrect)
                                .count();

                session.setStatus(QuizSession.SessionStatus.COMPLETED);
                session.setTotalCorrect(totalCorrect);
                session.setCompletedAt(Instant.now());
                sessionRepository.save(session);

                // FIX: avoid divide-by-zero when user completes with 0 answered questions
                double accuracy = attempts.isEmpty()
                                ? 0.0
                                : (double) totalCorrect / attempts.size() * 100;

                long totalTimeMs = attempts.stream()
                                .mapToLong(QuizAttempt::getTimeTakenMs)
                                .sum();

                // Fetch updated profile to show DNA changes
                LearningProfile profile = profileRepository.findByUserId(userId).orElse(null);

                log.info("Session completed — user:{} score:{}/{} accuracy:{}%",
                                userId, totalCorrect, attempts.size(), Math.round(accuracy));

                return QuizResultResponse.builder()
                                .sessionId(sessionId)
                                .conceptName(session.getConceptName())
                                .totalQuestions(attempts.size())
                                .totalCorrect(totalCorrect)
                                .accuracyPercent(Math.round(accuracy * 10.0) / 10.0)
                                .timeTakenMs(totalTimeMs)
                                .difficulty(session.getDifficulty())
                                .updatedDifficulty(profile != null
                                                ? profile.getCurrentDifficulty()
                                                : session.getDifficulty())
                                .difficultyChanged(profile != null &&
                                                !profile.getCurrentDifficulty().equals(session.getDifficulty()))
                                .build();
        }
        // ─── Get session history ──────────────────────────────────────────────

        public List<QuizSessionResponse> getHistory(UUID userId) {
                return sessionRepository
                                .findTop10ByUserIdOrderByStartedAtDesc(userId)
                                .stream()
                                .map(s -> mapToSessionResponse(s, false))
                                .collect(Collectors.toList());
        }

        // ─── Helpers ─────────────────────────────────────────────────────────

        private QuizSession getActiveSession(UUID sessionId, UUID userId) {
                QuizSession session = sessionRepository.findById(sessionId)
                                .orElseThrow(() -> AppException.notFound("Quiz session not found"));

                if (!session.getUser().getId().equals(userId)) {
                        throw AppException.forbidden("Not your session");
                }

                if (session.getStatus() != QuizSession.SessionStatus.IN_PROGRESS) {
                        throw AppException.badRequest("Session is already " + session.getStatus());
                }
                return session;
        }

        private int countHintsUsed(UUID sessionId, int questionIndex) {
                String key = HINT_KEY + sessionId + ":" + questionIndex;
                try {
                        Object val = redisTemplate.opsForValue().get(key);
                        if (val == null)
                                return 0;
                        return Integer.parseInt(val.toString());
                } catch (Exception e) {
                        log.warn("Failed to count hints for session {}, q{}: {}", sessionId, questionIndex,
                                        e.getMessage());
                        return 0;
                }
        }

        public void trackHintUsed(UUID sessionId, int questionIndex) {
                String key = HINT_KEY + sessionId + ":" + questionIndex;
                try {
                        redisTemplate.opsForValue().increment(key);
                        redisTemplate.expire(key, java.time.Duration.ofHours(24));
                } catch (Exception e) {
                        log.warn("Failed to track hint: {}", e.getMessage());
                }
        }

        private User getUser(UUID userId) {
                return userRepository.findById(userId)
                                .orElseThrow(() -> AppException.notFound("User not found"));
        }

        private QuizSessionResponse mapToSessionResponse(QuizSession session,
                        boolean includeAnswers) {
                List<QuizQuestionPublicDto> questions = session.getQuestions()
                                .stream()
                                .map(q -> QuizQuestionPublicDto.builder()
                                                .questionNumber(q.getQuestionNumber())
                                                .question(q.getQuestion())
                                                .options(q.getOptions())
                                                // Only expose correct answer when session is done
                                                .correctAnswerIndex(
                                                                includeAnswers || session
                                                                                .getStatus() == QuizSession.SessionStatus.COMPLETED
                                                                                                ? q.getCorrectAnswerIndex()
                                                                                                : -1)
                                                .build())
                                .collect(Collectors.toList());

                return QuizSessionResponse.builder()
                                .sessionId(session.getId())
                                .conceptName(session.getConceptName())
                                .difficulty(session.getDifficulty())
                                .learningStyle(session.getLearningStyle())
                                .questions(questions)
                                .status(session.getStatus().name())
                                .totalQuestions(session.getTotalQuestions())
                                .startedAt(session.getStartedAt())
                                .build();
        }
}