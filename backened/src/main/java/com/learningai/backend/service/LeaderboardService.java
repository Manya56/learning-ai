package com.learningai.backend.service;

import com.learningai.backend.repository.UserRepository;
import com.learningai.backend.repository.XpTransactionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ZSetOperations;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class LeaderboardService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final UserRepository                userRepository;
    private final XpTransactionRepository       xpRepository;
    private final XpService                     xpService;

    // ─── Weekly leaderboard — top 20 ─────────────────────────────────────

    public LeaderboardResponse getWeeklyLeaderboard(UUID requestingUserId) {
        String week    = xpService.getCurrentWeek();
        String key     = "leaderboard:weekly:" + week;

        List<LeaderboardEntry> top20 = fetchTopEntries(key, 20);

        // Always include requesting user's entry even if not in top 20
        LeaderboardEntry myEntry = buildUserEntry(requestingUserId, key, top20);

        return LeaderboardResponse.builder()
                .week(week)
                .entries(top20)
                .myEntry(myEntry)
                .totalParticipants(getTotalParticipants(key))
                .build();
    }

    // ─── All-time leaderboard — top 20 ───────────────────────────────────

    public LeaderboardResponse getAllTimeLeaderboard(UUID requestingUserId) {
        String key = "leaderboard:alltime";

        List<LeaderboardEntry> top20 = fetchTopEntries(key, 20);
        LeaderboardEntry myEntry = buildUserEntry(requestingUserId, key, top20);

        return LeaderboardResponse.builder()
                .week("all-time")
                .entries(top20)
                .myEntry(myEntry)
                .totalParticipants(getTotalParticipants(key))
                .build();
    }

    // ─── Fetch top N entries from Redis sorted set ────────────────────────

    private List<LeaderboardEntry> fetchTopEntries(String key, int count) {
        try {
            Set<ZSetOperations.TypedTuple<Object>> raw =
                    redisTemplate.opsForZSet()
                            .reverseRangeWithScores(key, 0, count - 1);

            if (raw == null || raw.isEmpty()) return List.of();

            List<LeaderboardEntry> entries = new ArrayList<>();
            int rank = 1;

            for (ZSetOperations.TypedTuple<Object> tuple : raw) {
                String userId = tuple.getValue() != null
                        ? tuple.getValue().toString() : null;
                if (userId == null) continue;

                int xp = tuple.getScore() != null
                        ? tuple.getScore().intValue() : 0;

                String displayName = resolveDisplayName(userId);
                String initials    = buildInitials(displayName);

                entries.add(LeaderboardEntry.builder()
                        .rank(rank++)
                        .userId(userId)
                        .displayName(displayName)
                        .initials(initials)
                        .xp(xp)
                        .rankBadge(getRankBadge(rank - 1))
                        .build());
            }
            return entries;

        } catch (Exception e) {
            log.warn("Redis leaderboard fetch failed: {}", e.getMessage());
            return List.of();
        }
    }

    // ─── Build the requesting user's entry ────────────────────────────────
    // Pinned at bottom of leaderboard even if not in top 20

    private LeaderboardEntry buildUserEntry(UUID userId,
                                             String key,
                                             List<LeaderboardEntry> top20) {
        // Check if already in top 20
        String userIdStr = userId.toString();
        Optional<LeaderboardEntry> inTop = top20.stream()
                .filter(e -> e.getUserId().equals(userIdStr))
                .findFirst();
        if (inTop.isPresent()) return inTop.get();

        // Not in top 20 — compute their rank and XP
        try {
            Long rank = redisTemplate.opsForZSet().reverseRank(key, userIdStr);
            Double score = redisTemplate.opsForZSet().score(key, userIdStr);

            int xp       = score != null ? score.intValue() : 0;
            long userRank = rank != null ? rank + 1 : top20.size() + 1;

            String name     = resolveDisplayName(userIdStr);
            String initials = buildInitials(name);

            return LeaderboardEntry.builder()
                    .rank((int) userRank)
                    .userId(userIdStr)
                    .displayName(name)
                    .initials(initials)
                    .xp(xp)
                    .rankBadge(null)
                    .isCurrentUser(true)
                    .build();

        } catch (Exception e) {
            log.warn("Failed to build user leaderboard entry: {}", e.getMessage());
            return LeaderboardEntry.builder()
                    .rank(-1).userId(userIdStr)
                    .displayName("You").initials("?")
                    .xp(0).isCurrentUser(true).build();
        }
    }

    // ─── Resolve display name from DB ─────────────────────────────────────

    private String resolveDisplayName(String userId) {
        try {
            return userRepository.findById(UUID.fromString(userId))
                    .map(u -> u.getFullName())
                    .orElse("Learner");
        } catch (Exception e) {
            return "Learner";
        }
    }

    private String buildInitials(String fullName) {
        if (fullName == null || fullName.isBlank()) return "?";
        String[] parts = fullName.trim().split("\\s+");
        if (parts.length == 1) return parts[0].substring(0, 1).toUpperCase();
        return (parts[0].substring(0, 1) + parts[parts.length - 1].substring(0, 1))
                .toUpperCase();
    }

    private String getRankBadge(int rank) {
        return switch (rank) {
            case 1 -> "🥇";
            case 2 -> "🥈";
            case 3 -> "🥉";
            default -> null;
        };
    }

    private long getTotalParticipants(String key) {
        try {
            Long size = redisTemplate.opsForZSet().size(key);
            return size != null ? size : 0;
        } catch (Exception e) {
            return 0;
        }
    }

    // ─── Response DTOs ─────────────────────────────────────────────────

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class LeaderboardResponse {
        private String week;
        private long totalParticipants;
        private List<LeaderboardEntry> entries;
        private LeaderboardEntry myEntry;
    }

    @lombok.Data @lombok.Builder @lombok.NoArgsConstructor @lombok.AllArgsConstructor
    public static class LeaderboardEntry {
        private int     rank;
        private String  userId;
        private String  displayName;
        private String  initials;
        private int     xp;
        private String  rankBadge;     // 🥇 🥈 🥉 or null
        private boolean isCurrentUser;
    }
}