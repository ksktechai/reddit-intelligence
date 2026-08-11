package com.example.reddit.analysis;

import java.util.List;

public interface AnalysisModel {
    ChunkAnalysis analyzeChunk(String model, AnalysisDatasetContext context, AnalysisChunk chunk);

    DatasetAnalysisResult synthesize(
            String model,
            AnalysisDatasetContext context,
            List<ChunkAnalysis> chunkAnalyses);
}
