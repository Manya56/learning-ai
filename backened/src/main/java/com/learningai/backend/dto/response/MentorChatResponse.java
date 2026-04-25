package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.UUID;

@Data @Builder
public class MentorChatResponse {

    private UUID sessionId;
    private String reply;
    private String personality;
    private int messageCount;

    // DNA context used for this response
    private String currentDifficulty;
    private String learningStyle;
    private Instant timestamp;
}