package com.learningai.backend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

import java.util.List;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TopicResponse implements Serializable {

    private UUID id;
    private String name;
    private String category;
    private String description;
    private Integer orderIndex;
    private Long conceptCount;
    private List<ConceptResponse> concepts;
}