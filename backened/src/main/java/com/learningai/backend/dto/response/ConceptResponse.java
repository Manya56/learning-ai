package com.learningai.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
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