package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class AnswerFeedbackResponse {
    private int questionIndex;
    private int selectedIndex;
    private int correctIndex;
    private boolean correct;
    private String explanation;
    private String conceptName;
}