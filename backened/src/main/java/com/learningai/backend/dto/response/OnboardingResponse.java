package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.UUID;

@Data
@Builder
public class OnboardingResponse {

    private UUID profileId;
    private String goal;
    private String preferredLanguage;
    private String currentDifficulty;
    private String learningStyle;
    private List<String> roadmapTopics;
    private String message;
}