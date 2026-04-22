package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data @Builder
public class HintResponse {
    private int questionIndex;
    private int hintNumber;
    private String hint;
}