package com.example.reddit.analysis;

import java.util.List;

public record ChunkAnalysis(
        List<ExtractedTopic> topics,
        List<ExtractedClaim> claims) {
}
