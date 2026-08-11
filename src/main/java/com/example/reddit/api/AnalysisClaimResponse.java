package com.example.reddit.api;

import com.example.reddit.persistence.ClaimType;
import com.example.reddit.persistence.Sentiment;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "AnalysisClaim", description = "A model-extracted claim retained only when supported by stored source text.")
public record AnalysisClaimResponse(
        @Schema(description = "Internal claim ID.", example = "25")
        long claimId,
        @Schema(description = "Normalized statement derived from one or more Reddit sources.")
        String text,
        @Schema(description = "Nature of the statement: experience, opinion, factual assertion, or recommendation.")
        ClaimType type,
        @Schema(description = "Qualitative sentiment expressed by the claim.")
        Sentiment sentiment,
        @Schema(description = "Model confidence clamped from 0 to 1.", example = "0.86")
        double confidence,
        @Schema(description = "Number of attached citations classified as supporting the claim.", example = "3")
        int supportCount,
        @Schema(description = "Number of attached citations classified as contradicting the claim.", example = "1")
        int contradictCount,
        @Schema(description = "Stored, source-linked excerpts retained after application validation.")
        List<AnalysisEvidenceResponse> evidence) {
}
