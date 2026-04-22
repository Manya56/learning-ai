package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class ProblemResponse {

    private String conceptName;
    private String difficulty;
    private String problemType;   // CODING / MATH / WRITTEN
    private String language;      // java / python / math / text
    private String problemStatement;
    private String starterCode;
    private List<TestCase> testCases;
    private String constraints;
    private List<String> hints;

    @Data @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class TestCase {
        private String input;
        private String expectedOutput;
    }
}