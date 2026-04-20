package com.learningai.backend.dto.request;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

@Data
public class KnowledgeQuizAnswerRequest {

    @NotNull
    private String goal;

    // List of selected answer indexes (0-3) for each question
    @NotEmpty(message = "Answers are required")
    private List<Integer> answers;

    // The correct answers (sent from the quiz generation step)
    @NotEmpty(message = "Correct answers are required")
    private List<Integer> correctAnswers;
}