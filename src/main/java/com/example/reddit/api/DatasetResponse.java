package com.example.reddit.api;

import com.example.reddit.persistence.DatasetStatus;

import java.time.Instant;

public record DatasetResponse(
        long datasetId,
        String subreddit,
        String query,
        String sort,
        String timeRange,
        int maxPosts,
        boolean includeComments,
        int postsImported,
        int commentsImported,
        DatasetStatus status,
        Instant createdAt,
        Instant completedAt,
        String errorMessage) {
}
