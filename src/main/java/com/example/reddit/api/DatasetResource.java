package com.example.reddit.api;

import com.example.reddit.dataset.DatasetService;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.openapi.annotations.Operation;
import org.eclipse.microprofile.openapi.annotations.media.Content;
import org.eclipse.microprofile.openapi.annotations.media.Schema;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse;
import org.eclipse.microprofile.openapi.annotations.responses.APIResponses;
import org.eclipse.microprofile.openapi.annotations.parameters.Parameter;
import org.eclipse.microprofile.openapi.annotations.tags.Tag;

import java.util.List;

@Path("/api/datasets")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Datasets", description = "Import Reddit search results through Crawlora and inspect the stored datasets.")
public class DatasetResource {
    private final DatasetService datasetService;

    @Inject
    public DatasetResource(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @POST
    @Operation(
            operationId = "createDataset",
            summary = "Import a Reddit dataset",
            description = "Searches one subreddit through Crawlora, stores matching posts on or after "
                    + "fromDate, and optionally imports their comments. The import runs synchronously: "
                    + "the response contains the final COMPLETED or FAILED status and any provider error.")
    @APIResponses({
            @APIResponse(
                    responseCode = "201",
                    description = "Dataset import finished and the dataset record was created.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON,
                            schema = @Schema(implementation = DatasetResponse.class))),
            @APIResponse(responseCode = "400", description = "The request body or one of its fields is invalid.")
    })
    public Response create(@Valid CreateDatasetRequest request) {
        DatasetResponse dataset = datasetService.create(request);
        return Response.status(Response.Status.CREATED).entity(dataset).build();
    }

    @GET
    @Operation(
            operationId = "listDatasets",
            summary = "List datasets",
            description = "Returns every dataset import, newest first, including its search criteria, status, "
                    + "imported record counts, timestamps, and failure message when applicable.")
    @APIResponse(responseCode = "200", description = "Dataset summaries were returned successfully.")
    public List<DatasetResponse> list() {
        return datasetService.list();
    }

    @GET
    @Path("/{datasetId}")
    @Operation(
            operationId = "getDataset",
            summary = "Get a dataset",
            description = "Returns the stored import configuration, completion status, counts, timestamps, "
                    + "and provider error for one dataset.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "The dataset was found."),
            @APIResponse(responseCode = "404", description = "No dataset exists with the supplied ID.")
    })
    public DatasetResponse get(
            @Parameter(description = "Internal dataset ID.", example = "1", required = true)
            @PathParam("datasetId") long datasetId) {
        return datasetService.get(datasetId);
    }

    @GET
    @Path("/{datasetId}/posts")
    @Operation(
            operationId = "listDatasetPosts",
            summary = "List posts in a dataset",
            description = "Returns the Reddit posts persisted for this dataset. This reads stored data only "
                    + "and does not make a new Crawlora request.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Stored posts were returned; the list may be empty."),
            @APIResponse(responseCode = "404", description = "No dataset exists with the supplied ID.")
    })
    public List<PostResponse> posts(
            @Parameter(description = "Internal dataset ID.", example = "1", required = true)
            @PathParam("datasetId") long datasetId) {
        return datasetService.posts(datasetId);
    }
}
