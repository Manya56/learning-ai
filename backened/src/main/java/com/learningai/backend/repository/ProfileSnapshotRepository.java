package com.learningai.backend.repository;

import com.learningai.backend.entity.ProfileSnapshot;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface ProfileSnapshotRepository
        extends JpaRepository<ProfileSnapshot, UUID> {

    // Last 10 snapshots for a user, newest first
    List<ProfileSnapshot> findTop10ByUserIdOrderByCreatedAtDesc(UUID userId);

    // All snapshots for a user
    List<ProfileSnapshot> findByUserIdOrderByCreatedAtAsc(UUID userId);
}