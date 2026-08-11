package com.example.reddit.analysis;

import java.util.List;

public record DatasetAnalysisResult(
        List<ExtractedTopic> topics,
        List<ExtractedClaim> claims,
        DecisionReport report) {
}
