package com.learningai.backend.service;

import com.learningai.backend.dto.request.EvaluateSubmissionRequest;
import com.learningai.backend.dto.request.GenerateProblemRequest;
import com.learningai.backend.dto.response.CodingAttemptResponse;
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

        private final AiService aiService;
        private final CodingAttemptRepository attemptRepository;
        private final LearningProfileRepository profileRepository;
        private final UserRepository userRepository;
        private final LearningProfileService profileService;
        private final RoadmapService roadmapService; // NEW

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
                                learningStyle);
        }

        // ─── Evaluate a submission ────────────────────────────────────────────

        @Transactional
        public EvaluationResult evaluateSubmission(UUID userId,
                        EvaluateSubmissionRequest request) {

                User user = getUser(userId);
                LearningProfile profile = getProfile(userId);

                log.info("Evaluating — user:{} concept:{} type:{}",
                                userId, request.getConceptName(), request.getProblemType());

                EvaluationResult result = aiService.evaluateSubmission(
                                request.getProblemStatement(),
                                request.getUserSubmission(),
                                request.getProblemType(),
                                request.getLanguage(),
                                request.getConceptName());

                CodingAttempt attempt = CodingAttempt.builder()
                                .user(user).conceptName(request.getConceptName())
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

                boolean correct = result.getScore() >= 6;
                profileService.recordAttempt(userId, request.getConceptName(),
                                correct, request.getTimeTakenMs(), false,
                                "CODING".equals(request.getProblemType()));

                // NEW: notify roadmap with practice score (score/10 as 0.0-1.0)
                try {
                        RoadmapService.ConceptCompleteResponse roadmapResult = roadmapService.markConceptComplete(
                                        userId,
                                        request.getConceptName(),
                                        "PRACTICE",
                                        result.getScore() / 10.0);
                        result.setRoadmapTopicProgress(roadmapResult.getTopicProgressPercent());
                        result.setRoadmapMessage(roadmapResult.getMessage());
                        result.setNextConceptToStudy(roadmapResult.getNextConceptToStudy());
                } catch (Exception e) {
                        log.warn("Roadmap update after practice failed (non-fatal): {}", e.getMessage());
                }

                log.info("Evaluated — score:{}/10 passed:{} user:{}", result.getScore(), result.isPassed(), userId);
                return result;
        }

        // ─── Get attempt history ──────────────────────────────────────────────

        public List<CodingAttemptResponse> getHistory(UUID userId) {
                return attemptRepository
                                .findTop10ByUserIdOrderByAttemptedAtDesc(userId)
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        // ─── Get attempts for a specific concept ─────────────────────────────

        public List<CodingAttemptResponse> getConceptHistory(UUID userId,
                        String conceptName) {
                return attemptRepository
                                .findByUserIdAndConceptNameOrderByAttemptedAtDesc(
                                                userId, conceptName)
                                .stream()
                                .map(this::mapToResponse)
                                .toList();
        }

        public CodingAttemptResponse getAttemptDetails(UUID userId, UUID attemptId) {
                CodingAttempt attempt = attemptRepository.findById(attemptId)
                                .orElseThrow(() -> AppException.notFound("Attempt not found"));

                if (!attempt.getUser().getId().equals(userId)) {
                        throw AppException.forbidden("Not your attempt");
                }

                return mapToResponse(attempt);
        }

        public CodingAttemptResponse mapToResponse(CodingAttempt attempt) {
                return CodingAttemptResponse.builder()
                                .id(attempt.getId())
                                .conceptName(attempt.getConceptName())
                                .problemStatement(attempt.getProblemStatement())
                                .problemType(attempt.getProblemType())
                                .language(attempt.getLanguage())
                                .userSubmission(attempt.getUserSubmission())
                                .score(attempt.getScore())
                                .feedback(attempt.getFeedback())
                                .passed(attempt.getPassed())
                                .timeTakenMs(attempt.getTimeTakenMs())
                                .difficulty(attempt.getDifficulty())
                                .attemptedAt(attempt.getAttemptedAt())
                                .build();
        }

        // ─── Helpers ─────────────────────────────────────────────────────────

        private String buildFeedbackText(EvaluationResult result) {
                return "STRENGTHS: " + result.getStrengths() +
                                "\nISSUES: " + result.getIssues() +
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