package com.example.reddit.analysis;

import java.time.LocalDate;

public record AnalysisDatasetContext(
        long datasetId,
        String subreddit,
        String query,
        LocalDate fromDate,
        int postCount,
        int commentCount) {
}
