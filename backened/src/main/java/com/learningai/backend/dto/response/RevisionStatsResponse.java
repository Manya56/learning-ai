package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data @Builder
public class RevisionStatsResponse {

    private long totalCards;
    private long dueToday;
    private long masteredCards;
    private long overdueCards;

    // Due cards with retention scores
    private List<RevisionCardResponse> dueCards;

    // Next 7 days forecast
    private List<DayForecast> weekForecast;

    @Data @Builder
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class DayForecast {
        private String date;
        private int cardsDue;
    }
}