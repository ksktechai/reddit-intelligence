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

import java.util.List;

@Path("/api/datasets")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class DatasetResource {
    private final DatasetService datasetService;

    @Inject
    public DatasetResource(DatasetService datasetService) {
        this.datasetService = datasetService;
    }

    @POST
    public Response create(@Valid CreateDatasetRequest request) {
        DatasetResponse dataset = datasetService.create(request);
        return Response.status(Response.Status.CREATED).entity(dataset).build();
    }

    @GET
    public List<DatasetResponse> list() {
        return datasetService.list();
    }

    @GET
    @Path("/{datasetId}")
    public DatasetResponse get(@PathParam("datasetId") long datasetId) {
        return datasetService.get(datasetId);
    }

    @GET
    @Path("/{datasetId}/posts")
    public List<PostResponse> posts(@PathParam("datasetId") long datasetId) {
        return datasetService.posts(datasetId);
    }
}
