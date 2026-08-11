package com.example.reddit.api;

import com.example.reddit.analysis.EvidenceSourceType;
import com.example.reddit.persistence.EvidenceStance;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "AnalysisEvidence", description = "A verified excerpt linked to a stored Reddit post or comment.")
public record AnalysisEvidenceResponse(
        @Schema(description = "Internal evidence-record ID.", example = "70")
        long evidenceId,
        @Schema(description = "Whether the citation references a stored POST or COMMENT.")
        EvidenceSourceType sourceType,
        @Schema(description = "Internal post or comment ID, interpreted using sourceType.", example = "42")
        long sourceId,
        @Schema(description = "Original Reddit base-36 identifier.", example = "1abcxyz")
        String redditId,
        @Schema(description = "Permalink to the source Reddit discussion.")
        String permalink,
        @Schema(description = "How the excerpt relates to the claim: SUPPORTS, CONTRADICTS, or CONTEXT.")
        EvidenceStance stance,
        @Schema(description = "Verbatim excerpt validated against the stored source text.")
        String excerpt,
        @Schema(description = "Short explanation of why the excerpt is relevant to the claim.")
        String rationale) {
}
