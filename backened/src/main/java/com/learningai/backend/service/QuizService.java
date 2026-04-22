package com.learningai.backend.service;

import com.learningai.backend.dto.response.*;
import com.learningai.backend.entity.*;
import com.learningai.backend.exception.AppException;
import com.learningai.backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuizService {

    private final QuizSessionRepository     sessionRepository;
    private final QuizAttemptRepository     attemptRepository;
    private final LearningProfileRepository profileRepository;
    private final UserRepository            userRepository;
    private final AiService                 aiService;
    private final LearningProfileService    profileService;

    private static final int QUESTIONS_PER_SESSION = 5;

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

        String difficulty    = profile.getCurrentDifficulty();
        String learningStyle = profile.getLearningStyle();

        log.info("Generating quiz for user:{} concept:{} difficulty:{} style:{}",
                userId, conceptName, difficulty, learningStyle);

        // Generate questions via AI (injecting DNA)
        List<QuizSession.QuizQuestionData> questions =
                aiService.generateQuizForConcept(
                    conceptName, difficulty, learningStyle,
                    QUESTIONS_PER_SESSION);

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

        // Return questions WITHOUT correct answer index (security)
        return mapToSessionResponse(session, false);
    }

    // ─── Submit a single answer ───────────────────────────────────────────

    @Transactional
    public AnswerFeedbackResponse submitAnswer(UUID userId,
                                               UUID sessionId,
                                               int questionIndex,
                                               int selectedIndex,
                                               long timeTakenMs) {

        QuizSession session = getActiveSession(sessionId, userId);
        User user = session.getUser();

        // Validate bounds
        if (questionIndex < 0 ||
            questionIndex >= session.getQuestions().size()) {
            throw AppException.badRequest("Invalid question index");
        }

        // Check not already answered
        List<QuizAttempt> existing =
                attemptRepository.findBySessionIdOrderByQuestionIndexAsc(
                        sessionId);
        boolean alreadyAnswered = existing.stream()
                .anyMatch(a -> a.getQuestionIndex() == questionIndex);
        if (alreadyAnswered) {
            throw AppException.conflict(
                "Question " + questionIndex + " already answered");
        }

        QuizSession.QuizQuestionData qData =
                session.getQuestions().get(questionIndex);

        boolean correct = (selectedIndex == qData.getCorrectAnswerIndex());
        int hintsUsed   = countHintsUsed(existing, questionIndex);

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

        return AnswerFeedbackResponse.builder()
                .questionIndex(questionIndex)
                .selectedIndex(selectedIndex)
                .correctIndex(qData.getCorrectAnswerIndex())
                .correct(correct)
                .explanation(qData.getExplanation())
                .conceptName(session.getConceptName())
                .build();
    }

    // ─── Get a hint for a question ────────────────────────────────────────

    public HintResponse getHint(UUID userId,
                                 UUID sessionId,
                                 int questionIndex,
                                 int hintNumber) {

        QuizSession session = getActiveSession(sessionId, userId);

        if (questionIndex < 0 ||
            questionIndex >= session.getQuestions().size()) {
            throw AppException.badRequest("Invalid question index");
        }

        if (hintNumber < 1 || hintNumber > 3) {
            throw AppException.badRequest("Hint number must be 1, 2, or 3");
        }

        QuizSession.QuizQuestionData qData =
                session.getQuestions().get(questionIndex);
        List<String> hints = qData.getHints();

        String hintText;
        // Use pre-generated hint if available, otherwise generate live
        if (hints != null && hints.size() >= hintNumber) {
            hintText = hints.get(hintNumber - 1);
        } else {
            hintText = aiService.generateHint(
                    qData.getQuestion(),
                    session.getConceptName(),
                    hintNumber);
        }

        log.info("Hint {} given — user:{} session:{} q:{}",
                hintNumber, userId, sessionId, questionIndex);

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

        List<QuizAttempt> attempts =
                attemptRepository.findBySessionIdOrderByQuestionIndexAsc(
                        sessionId);

        int totalCorrect = (int) attempts.stream()
                .filter(QuizAttempt::getCorrect).count();

        session.setStatus(QuizSession.SessionStatus.COMPLETED);
        session.setTotalCorrect(totalCorrect);
        session.setCompletedAt(Instant.now());
        sessionRepository.save(session);

        double accuracy = attempts.isEmpty() ? 0.0
                : (double) totalCorrect / attempts.size() * 100;

        // Fetch updated profile to show DNA changes
        LearningProfile profile = profileRepository
                .findByUserId(userId).orElse(null);

        log.info("Session completed — user:{} score:{}/{} accuracy:{}%",
                userId, totalCorrect, attempts.size(),
                Math.round(accuracy));

        return QuizResultResponse.builder()
                .sessionId(sessionId)
                .conceptName(session.getConceptName())
                .totalQuestions(attempts.size())
                .totalCorrect(totalCorrect)
                .accuracyPercent(Math.round(accuracy * 10.0) / 10.0)
                .timeTakenMs(attempts.stream()
                        .mapToLong(QuizAttempt::getTimeTakenMs).sum())
                .difficulty(session.getDifficulty())
                .updatedDifficulty(profile != null ?
                        profile.getCurrentDifficulty() : session.getDifficulty())
                .difficultyChanged(profile != null &&
                        !profile.getCurrentDifficulty()
                                .equals(session.getDifficulty()))
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
                .orElseThrow(() -> AppException.notFound(
                    "Quiz session not found"));

        if (!session.getUser().getId().equals(userId)) {
            throw AppException.forbidden("Not your session");
        }

        if (session.getStatus() != QuizSession.SessionStatus.IN_PROGRESS) {
            throw AppException.badRequest(
                "Session is already " + session.getStatus());
        }
        return session;
    }

    private int countHintsUsed(List<QuizAttempt> attempts,
                                int questionIndex) {
        // Hints are tracked separately — this is a placeholder
        // In Day 7 we track per-question hint count via Redis
        return 0;
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
                        // Only include correct answer if session is done
                        .correctAnswerIndex(includeAnswers ||
                            session.getStatus() == QuizSession.SessionStatus.COMPLETED
                                ? q.getCorrectAnswerIndex() : -1)
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