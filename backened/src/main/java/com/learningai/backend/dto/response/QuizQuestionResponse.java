package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QuizQuestionResponse {

    private int questionNumber;
    private String question;
    private List<String> options;
    private int correctAnswerIndex;
    private String explanation;
}