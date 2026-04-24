package com.learningai.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

import com.pgvector.PGvector;

@Entity
@Table(name = "content_embeddings",
       indexes = {
           @Index(name = "idx_embedding_content_id",
                  columnList = "content_id"),
           @Index(name = "idx_embedding_concept_tag",
                  columnList = "concept_tag")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ContentEmbedding {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Reference to the scraped content
    @Column(name = "content_id", nullable = false)
    private UUID contentId;

    // Denormalized for fast filtered search
    @Column(name = "concept_tag", nullable = false)
    private String conceptTag;

    @Column
    private String conceptName;

    // The text chunk that was embedded
    // (could be a sub-chunk of the full bodyText)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String chunkText;

    // Chunk index if content was split into multiple chunks
    @Column(nullable = false)
    private Integer chunkIndex;

    // The actual vector — 768 dimensions for nomic-embed-text
    @Column(columnDefinition = "vector(384)", nullable = false)
    private float[] embedding;

    // Model used
    @Column(nullable = false)
    private String model;

    // Source URL for citation in RAG responses
    @Column(length = 2048)
    private String sourceUrl;

    // Source title for citation
    @Column
    private String sourceTitle;

    @Column(nullable = false, updatable = false)
    private Instant createdAt;

    @PrePersist
    public void prePersist() {
        createdAt = Instant.now();
        if (chunkIndex == null) chunkIndex = 0;
    }
}