package com.example.reddit.reddit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.annotation.RegisterProvider;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@Path("/api/v1/reddit")
@RegisterRestClient(configKey = "crawlora-api")
@RegisterProvider(CrawloraHttpLoggingFilter.class)
public interface CrawloraApi {

    @GET
    @Path("/search")
    Response search(
            @QueryParam("q") String query,
            @QueryParam("subreddit") String subreddit,
            @QueryParam("sort") String sort,
            @QueryParam("time") String timeRange,
            @QueryParam("limit") int limit,
            @QueryParam("after") String after,
            @HeaderParam("x-api-key") String apiKey);

    @GET
    @Path("/comments/{postId}")
    Response comments(
            @PathParam("postId") String postId,
            @QueryParam("sort") String sort,
            @QueryParam("limit") int limit,
            @HeaderParam("x-api-key") String apiKey);
}
