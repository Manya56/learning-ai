package com.learningai.backend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
public class ConceptResponse implements Serializable{

    private UUID id;
    private String name;
    private String description;
    private String difficultyLevel;
    private Integer orderIndex;
    private Integer estimatedMinutes;
    private String tags;
    private UUID topicId;
    private String topicName;
}