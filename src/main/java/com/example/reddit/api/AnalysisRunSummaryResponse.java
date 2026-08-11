package com.example.reddit.api;

import com.example.reddit.persistence.AnalysisStatus;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.time.Instant;

@Schema(name = "AnalysisRunSummary", description = "Lifecycle and result counts for one immutable analysis run.")
public record AnalysisRunSummaryResponse(
        @Schema(description = "Internal analysis-run ID used for polling.", example = "1")
        long analysisId,
        @Schema(description = "Dataset analysed by this run.", example = "1")
        long datasetId,
        @Schema(description = "Current lifecycle state: PENDING, RUNNING, COMPLETED, or FAILED.")
        AnalysisStatus status,
        @Schema(description = "Ollama model selected for this run.", example = "gpt-oss:20b")
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
        @Schema(description = "Number of stored posts and comments supplied to analysis.", example = "450")
        int inputSourceCount,
        @Schema(description = "Number of bounded input chunks processed.", example = "8")
        int chunkCount,
        @Schema(description = "Number of persisted topics.", example = "12")
        int topicCount,
        @Schema(description = "Number of persisted evidence-backed claims.", example = "38")
        int claimCount,
        @Schema(description = "Number of persisted source citations.", example = "74")
        int evidenceCount) {
}
