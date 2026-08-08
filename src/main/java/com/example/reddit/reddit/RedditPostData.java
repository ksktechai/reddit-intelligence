package com.example.reddit.reddit;

import java.time.Instant;

public record RedditPostData(
        String redditId,
        String subreddit,
        String title,
        String body,
        String author,
        int score,
        String permalink,
        String externalUrl,
        Instant createdAt,
        int commentCountReported) {
}
