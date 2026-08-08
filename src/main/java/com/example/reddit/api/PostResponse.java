package com.example.reddit.api;

import java.time.Instant;

public record PostResponse(
        long id,
        String redditId,
        String subreddit,
        String title,
        String body,
        String author,
        int score,
        String permalink,
        String externalUrl,
        Instant createdAt,
        Instant collectedAt,
        int commentCountReported,
        boolean commentsDownloaded,
        boolean commentsComplete) {
}
