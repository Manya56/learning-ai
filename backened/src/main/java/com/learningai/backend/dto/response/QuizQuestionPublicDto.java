package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class QuizQuestionPublicDto {
    private int questionNumber;
    private String question;
    private List<String> options;
    private int correctAnswerIndex; // -1 when hidden during active session
}