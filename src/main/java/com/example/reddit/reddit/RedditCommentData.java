package com.example.reddit.reddit;

import java.time.Instant;
import java.util.List;

public record RedditCommentData(
        String redditId,
        String author,
        String body,
        int score,
        int depth,
        Instant createdAt,
        boolean deleted,
        List<RedditCommentData> replies) {

    public RedditCommentData {
        replies = replies == null ? List.of() : List.copyOf(replies);
    }
}
