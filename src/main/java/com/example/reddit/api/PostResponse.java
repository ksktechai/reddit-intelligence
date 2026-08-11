package com.example.reddit.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "RedditPost", description = "A Reddit post normalized and stored during dataset import.")
public record PostResponse(
        @Schema(description = "Internal database ID used by this API.", example = "42")
        long id,
        @Schema(description = "Reddit base-36 post ID.", example = "1abcxyz")
        String redditId,
        @Schema(description = "Subreddit without the r/ prefix.", example = "PersonalFinanceNZ")
        String subreddit,
        @Schema(description = "Post title.")
        String title,
        @Schema(description = "Self-post body; may be empty for link posts.")
        String body,
        @Schema(description = "Reddit author name as observed at collection time.", example = "sample_user")
        String author,
        @Schema(description = "Reddit score observed at collection time.", example = "18")
        int score,
        @Schema(description = "Reddit permalink for the post.")
        String permalink,
        @Schema(description = "External target URL for link posts, when present.")
        String externalUrl,
        @Schema(description = "Reddit post creation time.")
        Instant createdAt,
        @Schema(description = "Time this application collected the post.")
        Instant collectedAt,
        @Schema(description = "Comment count reported by Reddit/Crawlora.", example = "24")
        int commentCountReported,
        @Schema(description = "Whether comment collection was attempted.")
        boolean commentsDownloaded,
        @Schema(description = "Whether Crawlora indicated that the stored comment thread is complete.")
        boolean commentsComplete) {
}
