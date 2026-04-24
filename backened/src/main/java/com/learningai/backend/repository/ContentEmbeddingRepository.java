package com.learningai.backend.repository;

import com.learningai.backend.entity.ContentEmbedding;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ContentEmbeddingRepository
                extends JpaRepository<ContentEmbedding, UUID> {

        boolean existsByContentIdAndChunkIndex(UUID contentId, int chunkIndex);

        List<ContentEmbedding> findByConceptTagIgnoreCase(String conceptTag);

        long countByConceptTagIgnoreCase(String conceptTag);

        // ── Core semantic search using pgvector cosine distance ──────────────
        // <=> is cosine distance in pgvector (lower = more similar)
        // 1 - (embedding <=> queryVector) = cosine similarity

        @Query(value = """
                        SELECT ce.id, ce.chunk_index, ce.chunk_text, ce.concept_name,
                               ce.concept_tag, ce.content_id, ce.created_at, ce.model,
                               ce.source_title, ce.source_url
                        FROM content_embeddings ce
                        WHERE LOWER(ce.concept_tag) = LOWER(:conceptTag)
                        ORDER BY ce.embedding <=> CAST(:queryVector AS vector)
                        LIMIT :limit
                        """, nativeQuery = true)
        List<ContentEmbeddingProjection> findSimilar(
                        @Param("queryVector") String queryVector,
                        @Param("conceptTag") String conceptTag,
                        @Param("limit") int limit);

        @Query(value = """
                        SELECT ce.id, ce.chunk_index, ce.chunk_text, ce.concept_name,
                               ce.concept_tag, ce.content_id, ce.created_at, ce.model,
                               ce.source_title, ce.source_url
                        FROM content_embeddings ce
                        ORDER BY ce.embedding <=> CAST(:queryVector AS vector)
                        LIMIT :limit
                        """, nativeQuery = true)
        List<ContentEmbeddingProjection> findSimilarGlobal(
                        @Param("queryVector") String queryVector,
                        @Param("limit") int limit);

        // Delete all embeddings for a content piece
        void deleteByContentId(UUID contentId);
}