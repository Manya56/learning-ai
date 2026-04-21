package com.learningai.backend.repository;

import com.learningai.backend.entity.Topic;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface TopicRepository extends JpaRepository<Topic, UUID> {

    List<Topic> findByCategoryOrderByOrderIndexAsc(String category);

    Optional<Topic> findByNameIgnoreCase(String name);

    boolean existsByName(String name);

    List<Topic> findAllByOrderByOrderIndexAsc();
}