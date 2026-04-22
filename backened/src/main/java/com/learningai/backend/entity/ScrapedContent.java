package com.learningai.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "scraped_content",
       indexes = {
           @Index(name = "idx_scraped_concept_tag",
                  columnList = "concept_tag"),
           @Index(name = "idx_scraped_url",
                  columnList = "url_hash")
       })
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor
@Builder
public class ScrapedContent {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // Full URL of the scraped page
    @Column(nullable = false, length = 2048)
    private String url;

    // MD5 hash of URL — for fast dedup lookup
    @Column(name = "url_hash", nullable = false, unique = true, length = 64)
    private String urlHash;

    // Page title
    @Column
    private String title;

    // Clean extracted text (no HTML)
    @Column(columnDefinition = "TEXT", nullable = false)
    private String bodyText;

    // Which topic goal this content belongs to
    // e.g. "DSA", "Machine Learning", "Stock Market"
    @Column(name = "concept_tag", nullable = false)
    private String conceptTag;

    // More specific concept within the topic
    // e.g. "Two Pointer", "Gradient Descent"
    @Column
    private String conceptName;

    // Source domain e.g. "geeksforgeeks.org"
    @Column
    private String source;

    // Word count of extracted text
    @Column
    private Integer wordCount;

    // Whether this content has been embedded into pgvector yet
    @Column(nullable = false)
    private Boolean embedded;

    // How many times this content was retrieved in RAG
    @Column(nullable = false)
    private Integer retrievalCount;

    // NULL = scraped by system, "USER" = triggered by user question
    @Column
    private String scrapeReason;

    @Column(nullable = false, updatable = false)
    private Instant scrapedAt;

    @Column
    private Instant lastAccessedAt;

    @PrePersist
    public void prePersist() {
        scrapedAt = Instant.now();
        if (embedded == null)        embedded       = false;
        if (retrievalCount == null)  retrievalCount = 0;
    }
}