package com.example.reddit.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.LocalDate;

@Schema(name = "CreateDatasetRequest", description = "Search and import settings for a new Reddit dataset.")
public record CreateDatasetRequest(
        @Schema(description = "Subreddit name without the r/ prefix.", example = "PersonalFinanceNZ")
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_]{2,100}$", message = "must be a valid subreddit name")
        String subreddit,

        @Schema(description = "Reddit search expression passed to Crawlora.", example = "\"graduate salary\" OR \"business analyst salary\"")
        @NotBlank
        @Size(max = 500)
        String query,

        @Schema(description = "Reddit result ordering.", example = "relevance", defaultValue = "relevance")
        @Pattern(regexp = "relevance|hot|top|new|comments")
        String sort,

        @Schema(description = "Reddit search time filter.", example = "all", defaultValue = "all")
        @Pattern(regexp = "all|hour|day|week|month|year")
        String timeRange,

        @Schema(description = "Inclusive application-side date filter. Posts older than this date are discarded.", example = "2023-01-01")
        @PastOrPresent
        LocalDate fromDate,

        @Schema(description = "Maximum number of matching posts to persist.", example = "50", defaultValue = "100")
        @Min(1) @Max(1000)
        Integer maxPosts,

        @Schema(description = "Whether to import stored comment threads for each selected post.", example = "true", defaultValue = "true")
        Boolean includeComments) {

    public CreateDatasetRequest {
        if (subreddit != null) {
            subreddit = subreddit.trim();
        }
        if (query != null) {
            query = query.trim();
        }
        sort = sort == null || sort.isBlank() ? "relevance" : sort;
        timeRange = timeRange == null || timeRange.isBlank() ? "all" : timeRange;
        maxPosts = maxPosts == null ? 100 : maxPosts;
        includeComments = includeComments == null || includeComments;
    }
}
