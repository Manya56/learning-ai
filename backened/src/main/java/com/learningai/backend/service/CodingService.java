package com.learningai.backend.service;

import com.learningai.backend.dto.request.EvaluateSubmissionRequest;
import com.learningai.backend.dto.request.GenerateProblemRequest;
import com.learningai.backend.dto.response.EvaluationResult;
import com.learningai.backend.dto.response.ProblemResponse;
import com.learningai.backend.entity.CodingAttempt;
import com.learningai.backend.entity.LearningProfile;
import com.learningai.backend.entity.User;
import com.learningai.backend.exception.AppException;
import com.learningai.backend.repository.CodingAttemptRepository;
import com.learningai.backend.repository.LearningProfileRepository;
import com.learningai.backend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class CodingService {

    private final AiService                 aiService;
    private final CodingAttemptRepository   attemptRepository;
    private final LearningProfileRepository profileRepository;
    private final UserRepository            userRepository;
    private final LearningProfileService    profileService;

    // ─── Generate a problem ───────────────────────────────────────────────

    public ProblemResponse generateProblem(UUID userId,
                                            GenerateProblemRequest request) {

        LearningProfile profile = getProfile(userId);

        // Use request values if provided, else fall back to DNA
        String difficulty = request.getDifficulty() != null
                ? request.getDifficulty()
                : profile.getCurrentDifficulty();

        String topicGoal = request.getTopicGoal() != null
                ? request.getTopicGoal()
                : profile.getGoal();

        String learningStyle = profile.getLearningStyle();

        log.info("Generating problem — user:{} concept:{} topic:{} diff:{}",
                userId, request.getConceptName(), topicGoal, difficulty);

        return aiService.generateProblem(
                request.getConceptName(),
                topicGoal,
                difficulty,
                learningStyle
        );
    }

    // ─── Evaluate a submission ────────────────────────────────────────────

    @Transactional
    public EvaluationResult evaluateSubmission(UUID userId,
                                                EvaluateSubmissionRequest request) {

        User user            = getUser(userId);
        LearningProfile profile = getProfile(userId);

        log.info("Evaluating submission — user:{} concept:{} type:{}",
                userId, request.getConceptName(), request.getProblemType());

        // Get AI evaluation
        EvaluationResult result = aiService.evaluateSubmission(
                request.getProblemStatement(),
                request.getUserSubmission(),
                request.getProblemType(),
                request.getLanguage(),
                request.getConceptName()
        );

        // Save the attempt to DB
        CodingAttempt attempt = CodingAttempt.builder()
                .user(user)
                .conceptName(request.getConceptName())
                .problemStatement(request.getProblemStatement())
                .problemType(request.getProblemType())
                .language(request.getLanguage())
                .userSubmission(request.getUserSubmission())
                .score(result.getScore())
                .feedback(buildFeedbackText(result))
                .passed(result.isPassed())
                .timeTakenMs(request.getTimeTakenMs())
                .difficulty(profile.getCurrentDifficulty())
                .build();

        attempt = attemptRepository.save(attempt);
        result.setAttemptId(attempt.getId());

        // Update Learning DNA — treat score >= 6 as correct
        boolean correct = result.getScore() >= 6;
        profileService.recordAttempt(
                userId,
                request.getConceptName(),
                correct,
                request.getTimeTakenMs(),
                false,  // no hints in coding problems
                "CODING".equals(request.getProblemType())
        );

        log.info("Submission evaluated — score:{}/10 passed:{} user:{}",
                result.getScore(), result.isPassed(), userId);

        return result;
    }

    // ─── Get attempt history ──────────────────────────────────────────────

    public List<CodingAttempt> getHistory(UUID userId) {
        return attemptRepository
                .findTop10ByUserIdOrderByAttemptedAtDesc(userId);
    }

    // ─── Get attempts for a specific concept ─────────────────────────────

    public List<CodingAttempt> getConceptHistory(UUID userId,
                                                   String conceptName) {
        return attemptRepository
                .findByUserIdAndConceptNameOrderByAttemptedAtDesc(
                        userId, conceptName);
    }

    // ─── Helpers ─────────────────────────────────────────────────────────

    private String buildFeedbackText(EvaluationResult result) {
        return "STRENGTHS: " + result.getStrengths() +
               "\nISSUES: "    + result.getIssues() +
               "\nSUGGESTIONS: " + result.getSuggestions();
    }

    private LearningProfile getProfile(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> AppException.notFound(
                    "Complete onboarding first"));
    }

    private User getUser(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> AppException.notFound("User not found"));
    }
}