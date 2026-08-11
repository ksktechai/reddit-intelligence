package com.example.reddit.analysis;

import java.time.Instant;

public record AnalysisSourceItem(
        EvidenceSourceType sourceType,
        long sourceId,
        long postId,
        String redditId,
        String permalink,
        String author,
        int score,
        Instant createdAt,
        String text) {

    public String reference() {
        return sourceType + ":" + sourceId;
    }
}
