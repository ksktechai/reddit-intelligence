package com.example.reddit.api;

import com.example.reddit.persistence.Sentiment;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "AnalysisTopic", description = "A recurring subject identified across the dataset.")
public record AnalysisTopicResponse(
        @Schema(description = "Internal topic ID.", example = "10")
        long topicId,
        @Schema(description = "Concise normalized topic name.", example = "Graduate salaries")
        String name,
        @Schema(description = "Evidence-aware summary of the discussion around this topic.")
        String summary,
        @Schema(description = "Overall qualitative sentiment for the topic.")
        Sentiment sentiment,
        @Schema(description = "Sentiment score clamped from -1 (negative) to 1 (positive).", example = "0.15")
        double sentimentScore,
        @Schema(description = "Number of source blocks associated with the topic.", example = "14")
        int mentionCount,
        @Schema(description = "Evidence-backed claims assigned to this topic.")
        List<AnalysisClaimResponse> claims) {
}
