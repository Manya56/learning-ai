package com.learningai.backend.repository;

import java.time.Instant;
import java.util.UUID;

public interface ContentEmbeddingProjection {
    UUID getId();
    UUID getContentId();
    String getConceptTag();
    String getConceptName();
    String getChunkText();
    Integer getChunkIndex();
    String getModel();
    String getSourceUrl();
    String getSourceTitle();
    Instant getCreatedAt();
}