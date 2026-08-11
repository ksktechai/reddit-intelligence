package com.example.reddit.analysis;

import java.util.List;

public record AnalysisDatasetSource(
        AnalysisDatasetContext context,
        List<AnalysisSourceItem> sources) {
}
