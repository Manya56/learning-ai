package com.learningai.backend.dto.response;

import com.learningai.backend.entity.MentorSession;
import lombok.Builder;
import lombok.Data;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data @Builder
public class MentorHistoryResponse {

    private UUID sessionId;
    private String sessionTopic;
    private String status;
    private int messageCount;
    private Instant createdAt;
    private Instant lastMessageAt;
    private List<MentorSession.MentorMessage> messages;
}