package com.example.reddit.analysis;

import java.util.List;

public record AnalysisChunk(
        int number,
        int total,
        String content,
        List<AnalysisSourceItem> sources) {
}
