package com.example.reddit.api;

import com.example.reddit.persistence.DatasetStatus;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.time.LocalDate;

@Schema(name = "Dataset", description = "Stored Reddit import configuration, lifecycle state, and import counts.")
public record DatasetResponse(
        @Schema(description = "Internal dataset ID.", example = "1")
        long datasetId,
        @Schema(description = "Imported subreddit without the r/ prefix.", example = "PersonalFinanceNZ")
        String subreddit,
        @Schema(description = "Search expression used for the import.", example = "graduate salary")
        String query,
        @Schema(description = "Reddit result ordering used for the search.", example = "relevance")
        String sort,
        @Schema(description = "Reddit time filter used for the search.", example = "all")
        String timeRange,
        @Schema(description = "Inclusive minimum Reddit post date, when supplied.", example = "2023-01-01")
        LocalDate fromDate,
        @Schema(description = "Requested maximum number of posts.", example = "50")
        int maxPosts,
        @Schema(description = "Whether comment collection was requested.", example = "true")
        boolean includeComments,
        @Schema(description = "Number of posts stored successfully.", example = "32")
        int postsImported,
        @Schema(description = "Number of comments stored successfully.", example = "418")
        int commentsImported,
        @Schema(description = "Final import state. A FAILED dataset remains available for diagnostics.")
        DatasetStatus status,
        @Schema(description = "Time the dataset record was created.", example = "2026-08-09T02:50:06Z")
        Instant createdAt,
        @Schema(description = "Time the synchronous import finished.", example = "2026-08-09T02:51:40Z")
        Instant completedAt,
        @Schema(description = "Provider or import error when status is FAILED; otherwise null.")
        String errorMessage) {
}
