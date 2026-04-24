package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class ExplainResponse {

    private String answer;
    private String conceptName;
    private String topicGoal;

    // RETRIEVED / AI_KNOWLEDGE / SCRAPED_FRESH / AI_FALLBACK
    private String sourceType;

    // Was fresh content scraped for this answer?
    private boolean freshlyScraped;

    // Source articles used
    private List<SourceDto> sources;

    @Data @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SourceDto {
        private String title;
        private String url;
    }
}