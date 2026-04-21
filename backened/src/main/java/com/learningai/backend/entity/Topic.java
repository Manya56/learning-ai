package com.learningai.backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "topics")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Topic {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    // e.g. "Arrays", "Trees", "Dynamic Programming"
    @Column(nullable = false, unique = true)
    private String name;

    // e.g. "DSA", "Spring Boot", "System Design"
    @Column(nullable = false)
    private String category;

    @Column
    private String description;

    // Order in the roadmap — lower = earlier
    @Column(nullable = false)
    private Integer orderIndex;

    @OneToMany(mappedBy = "topic",
               cascade = CascadeType.ALL,
               fetch = FetchType.LAZY)
    private List<Concept> concepts;
}