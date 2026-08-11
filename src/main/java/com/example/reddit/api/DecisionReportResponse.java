package com.example.reddit.api;

import org.eclipse.microprofile.openapi.annotations.media.Schema;

import java.util.List;

@Schema(name = "DecisionReport", description = "Decision-oriented synthesis generated from validated topics, claims, and evidence.")
public record DecisionReportResponse(
        @Schema(description = "Concise overview of the strongest evidence-supported observations.")
        String executiveSummary,
        @Schema(description = "Most important patterns observed in the Reddit dataset.")
        List<String> keyFindings,
        @Schema(description = "Potential opportunities suggested by the discussion, not independently verified facts.")
        List<String> opportunities,
        @Schema(description = "Risks, uncertainties, and negative signals found in the discussion.")
        List<String> risks,
        @Schema(description = "Practical next steps derived from the observed discussion.")
        List<String> recommendations,
        @Schema(description = "Mandatory methodological caveats plus model-identified limitations.")
        List<String> limitations) {
}
