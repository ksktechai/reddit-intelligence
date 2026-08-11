package com.example.reddit.api;

import com.example.reddit.analysis.AnalysisService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriInfo;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.headers.Header;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.parameters.RequestBody;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.net.URI;
import java.util.List;

@Path("/api/datasets/{datasetId}/analyses")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Analyses", description = "Run and inspect evidence-grounded local-model analyses of stored datasets.")
public class DatasetAnalysisResource {
    private final AnalysisService analysisService;

    @Inject
    public DatasetAnalysisResource(AnalysisService analysisService) {
        this.analysisService = analysisService;
    }

    @POST
    @Operation(
            operationId = "startDatasetAnalysis",
            summary = "Start a dataset analysis",
            description = "Queues an asynchronous local-model analysis of a completed dataset. The dataset "
                    + "must contain at least one post, and only one PENDING or RUNNING analysis is allowed per "
                    + "dataset. Returns HTTP 202 with a Location header for polling the new analysis.")
    @APIResponses({
            @APIResponse(
                    responseCode = "202",
                    description = "The analysis was created and queued.",
                    headers = @Header(
                            name = "Location",
                            description = "Absolute URL used to poll this analysis run.",
                            schema = @Schema(type = org.eclipse.microprofile.openapi.annotations.enums.SchemaType.STRING)),
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = AnalysisRunSummaryResponse.class))),
            @APIResponse(responseCode = "400", description = "The dataset has no posts or the request is invalid."),
            @APIResponse(responseCode = "404", description = "No dataset exists with the supplied ID."),
            @APIResponse(responseCode = "409", description = "The dataset is incomplete or already has an active analysis.")
    })
    public Response start(
            @Parameter(description = "Internal ID of the completed dataset to analyse.", example = "1", required = true)
            @PathParam("datasetId") long datasetId,
            @RequestBody(
                    description = "Optional model override. Send an empty object to use the configured default model.",
                    required = false)
            @Valid CreateAnalysisRequest request,
            @Context UriInfo uriInfo) {
        AnalysisRunSummaryResponse run = analysisService.start(datasetId, request);
        URI location = uriInfo.getBaseUriBuilder()
                .path("api/analyses/{analysisId}")
                .build(run.analysisId());
        return Response.accepted(run).location(location).build();
    }

    @GET
    @Operation(
            operationId = "listDatasetAnalyses",
            summary = "List analyses for a dataset",
            description = "Returns the immutable analysis-run history for the dataset, newest first. Each entry "
                    + "contains lifecycle timestamps, counts, model information, and any failure message.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Analysis-run summaries were returned successfully."),
            @APIResponse(responseCode = "404", description = "No dataset exists with the supplied ID.")
    })
    public List<AnalysisRunSummaryResponse> list(
            @Parameter(description = "Internal dataset ID.", example = "1", required = true)
            @PathParam("datasetId") long datasetId) {
        return analysisService.listForDataset(datasetId);
    }
}
