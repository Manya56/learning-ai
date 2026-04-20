package com.learningai.backend.repository;

import com.learningai.backend.entity.LearningProfile;
import com.learningai.backend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface LearningProfileRepository extends JpaRepository<LearningProfile, UUID> {

    Optional<LearningProfile> findByUser(User user);

    Optional<LearningProfile> findByUserId(UUID userId);

    boolean existsByUser(User user);

    @Query("SELECT lp FROM LearningProfile lp " +
           "JOIN FETCH lp.user " +
           "WHERE lp.user.id = :userId")
    Optional<LearningProfile> findByUserIdWithUser(UUID userId);
}