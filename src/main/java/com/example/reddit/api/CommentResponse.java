package com.example.reddit.api;

import java.time.Instant;

public record CommentResponse(
        long id,
        String redditId,
        long postId,
        Long parentCommentId,
        String author,
        String body,
        int score,
        int depth,
        Instant createdAt,
        Instant collectedAt,
        boolean deleted) {
}
