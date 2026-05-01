package com.learningai.backend.repository;

import com.learningai.backend.entity.XpTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface XpTransactionRepository extends JpaRepository<XpTransaction, UUID> {

    // Total XP for a user in a specific week
    @Query("SELECT COALESCE(SUM(x.xpEarned), 0) FROM XpTransaction x " +
           "WHERE x.user.id = :userId AND x.weekNumber = :week")
    int sumXpByUserAndWeek(@Param("userId") UUID userId,
                            @Param("week") String week);

    // Total XP for a user all-time
    @Query("SELECT COALESCE(SUM(x.xpEarned), 0) FROM XpTransaction x " +
           "WHERE x.user.id = :userId")
    int sumXpByUser(@Param("userId") UUID userId);

    // Recent XP events for a user
    List<XpTransaction> findTop20ByUserIdOrderByCreatedAtDesc(UUID userId);
}