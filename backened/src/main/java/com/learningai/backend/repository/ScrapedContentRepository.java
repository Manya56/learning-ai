package com.learningai.backend.repository;

import com.learningai.backend.entity.ScrapedContent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.data.repository.query.Param;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface ScrapedContentRepository
                extends JpaRepository<ScrapedContent, UUID> {

        // Fast dedup check before scraping
        boolean existsByUrlHash(String urlHash);

        Optional<ScrapedContent> findByUrlHash(String urlHash);

        // Find all unembedded content — for batch embedding job
        List<ScrapedContent> findByEmbeddedFalseOrderByScrapedAtAsc();

        // Find all content for a topic — used by RAG
        List<ScrapedContent> findByConceptTagIgnoreCaseOrderByRetrievalCountDesc(
                        String conceptTag);

        // Find by topic + concept
        List<ScrapedContent> findByConceptTagIgnoreCaseAndConceptNameIgnoreCase(
                        String conceptTag, String conceptName);

        // Count content per topic
        long countByConceptTagIgnoreCase(String conceptTag);

        // Most accessed content
        @Query("SELECT s FROM ScrapedContent s " +
                        "WHERE LOWER(s.conceptTag) = LOWER(:tag) " +
                        "ORDER BY s.retrievalCount DESC")
        List<ScrapedContent> findTopByConceptTag(String tag);

        // Add this method:
        @Query("SELECT s FROM ScrapedContent s " +
                        "WHERE s.scrapedAt < :threshold " +
                        "ORDER BY s.scrapedAt ASC")
        List<ScrapedContent> findByScrapedAtBefore(
                        @Param("threshold") Instant threshold);
}