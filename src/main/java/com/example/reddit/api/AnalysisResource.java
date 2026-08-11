package com.example.reddit.api;

import com.example.reddit.analysis.AnalysisService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

@Path("/api/analyses")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Analyses", description = "Run and inspect evidence-grounded local-model analyses of stored datasets.")
public class AnalysisResource {
    private final AnalysisService analysisService;

    @Inject
    public AnalysisResource(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @GET
    @Path("/{analysisId}")
    @Operation(
            operationId = "getAnalysis",
            summary = "Get analysis status and results",
            description = "Polls one analysis run. PENDING and RUNNING responses expose current lifecycle "
                    + "metadata; FAILED responses include errorMessage; COMPLETED responses include the decision "
                    + "report and nested topics, claims, and source-linked evidence.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "The analysis status and available results were returned."),
            @APIResponse(responseCode = "404", description = "No analysis exists with the supplied ID.")
    })
    public AnalysisResponse get(
            @Parameter(description = "Internal analysis-run ID returned by the start endpoint.", example = "1", required = true)
            @PathParam("analysisId") long analysisId) {
        return analysisService.get(analysisId);
    }
}
