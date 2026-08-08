package com.example.reddit.reddit;

import jakarta.ws.rs.GET;
import jakarta.ws.rs.HeaderParam;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RegisterRestClient;

@RegisterRestClient(configKey = "reddit-api")
public interface RedditJsonApi {

    @GET
    @Path("/r/{subreddit}/search.json")
    Response search(
            @PathParam("subreddit") String subreddit,
            @QueryParam("q") String query,
            @QueryParam("restrict_sr") String restrictSubreddit,
            @QueryParam("sort") String sort,
            @QueryParam("t") String timeRange,
            @QueryParam("limit") int limit,
            @QueryParam("after") String after,
            @QueryParam("raw_json") int rawJson,
            @HeaderParam("User-Agent") String userAgent);

    @GET
    @Path("/comments/{postId}.json")
    Response comments(
            @PathParam("postId") String postId,
            @QueryParam("limit") int limit,
            @QueryParam("raw_json") int rawJson,
            @HeaderParam("User-Agent") String userAgent);
}
