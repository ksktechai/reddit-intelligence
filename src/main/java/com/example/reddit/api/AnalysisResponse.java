package com.example.reddit.api;

import com.example.reddit.persistence.AnalysisStatus;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;
import java.util.List;

@Schema(name = "Analysis", description = "Full lifecycle state and evidence-grounded results for an analysis run.")
public record AnalysisResponse(
        @Schema(description = "Internal analysis-run ID.", example = "1")
        long analysisId,
        @Schema(description = "Internal dataset ID analysed by this run.", example = "1")
        long datasetId,
        @Schema(description = "Current lifecycle state: PENDING, RUNNING, COMPLETED, or FAILED.")
        AnalysisStatus status,
        @Schema(description = "Ollama model used for this run.", example = "gpt-oss:20b")
        String model,
        @Schema(description = "Application prompt contract used for reproducibility.", example = "phase2-v1")
        String promptVersion,
        @Schema(description = "Time the run was created.")
        Instant createdAt,
        @Schema(description = "Time model processing began; null while PENDING.")
        Instant startedAt,
        @Schema(description = "Time processing completed or failed; null while active.")
        Instant completedAt,
        @Schema(description = "Failure detail when status is FAILED; otherwise null.")
        String errorMessage,
        @Schema(description = "Number of stored posts and comments supplied to analysis.")
        int inputSourceCount,
        @Schema(description = "Number of bounded input chunks processed.")
        int chunkCount,
        @Schema(description = "Number of persisted topics.")
        int topicCount,
        @Schema(description = "Number of persisted evidence-backed claims.")
        int claimCount,
        @Schema(description = "Number of persisted source citations.")
        int evidenceCount,
        @Schema(description = "Decision-oriented synthesis; populated only after successful completion.")
        DecisionReportResponse report,
        @Schema(description = "Extracted topics with nested claims and verified evidence citations.")
        List<AnalysisTopicResponse> topics) {
}
