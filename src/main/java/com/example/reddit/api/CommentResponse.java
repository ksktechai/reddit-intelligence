package com.example.reddit.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "RedditComment", description = "A Reddit comment normalized and stored during dataset import.")
public record CommentResponse(
        @Schema(description = "Internal database ID used by this API.", example = "125")
        long id,
        @Schema(description = "Reddit base-36 comment ID.", example = "kzyx123")
        String redditId,
        @Schema(description = "Internal ID of the containing post.", example = "42")
        long postId,
        @Schema(description = "Internal parent-comment ID; null for a top-level comment.")
        Long parentCommentId,
        @Schema(description = "Reddit author name as observed at collection time.", example = "sample_user")
        String author,
        @Schema(description = "Comment body as collected from Reddit.")
        String body,
        @Schema(description = "Reddit score observed at collection time.", example = "7")
        int score,
        @Schema(description = "Zero-based nesting depth in the comment thread.", example = "0")
        int depth,
        @Schema(description = "Reddit comment creation time.")
        Instant createdAt,
        @Schema(description = "Time this application collected the comment.")
        Instant collectedAt,
        @Schema(description = "Whether the source comment was marked deleted.")
        boolean deleted) {
}
