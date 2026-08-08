package com.example.reddit.reddit;

import java.util.List;

public record RedditCommentThread(
        List<RedditCommentData> comments,
        boolean complete,
        int moreObjects) {

    public RedditCommentThread {
        comments = comments == null ? List.of() : List.copyOf(comments);
    }

    public int totalComments() {
        return comments.stream().mapToInt(RedditCommentThread::count).sum();
    }

    private static int count(RedditCommentData comment) {
        return 1 + comment.replies().stream().mapToInt(RedditCommentThread::count).sum();
    }
}
