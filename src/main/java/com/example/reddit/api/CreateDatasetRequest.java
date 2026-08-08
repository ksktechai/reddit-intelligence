package com.example.reddit.api;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record CreateDatasetRequest(
        @NotBlank
        @Pattern(regexp = "^[A-Za-z0-9_]{2,100}$", message = "must be a valid subreddit name")
        String subreddit,

        @NotBlank
        @Size(max = 500)
        String query,

        @Pattern(regexp = "relevance|hot|top|new|comments")
        String sort,

        @Pattern(regexp = "all|hour|day|week|month|year")
        String timeRange,

        @PastOrPresent
        LocalDate fromDate,

        @Min(1) @Max(1000)
        Integer maxPosts,

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
