package com.learningai.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.UUID;

@Entity
@Table(name = "concepts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Concept {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // e.g. "Two Pointer Technique", "Sliding Window"
    @Column(nullable = false)
    private String name;

    // Short explanation shown to user
    @Column(columnDefinition = "TEXT")
    private String description;

    // EASY, MEDIUM, HARD
    @Column(nullable = false)
    private String difficultyLevel;

    // Order within the topic
    @Column(nullable = false)
    private Integer orderIndex;

    // Estimated minutes to learn
    @Column(nullable = false)
    private Integer estimatedMinutes;

    // Tags for search — e.g. "array,pointer,optimization"
    @Column
    private String tags;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "topic_id", nullable = false)
    private Topic topic;
}