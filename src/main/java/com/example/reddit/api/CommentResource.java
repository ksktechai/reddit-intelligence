package com.example.reddit.api;

import com.example.reddit.comment.CommentService;
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

@Path("/api/comments")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Reddit Content", description = "Inspect Reddit posts and comments already stored in the database.")
public class CommentResource {
    private final CommentService commentService;

    @Inject
    public CommentResource(CommentService commentService) {
        this.commentService = commentService;
    }

    @GET
    @Path("/{commentId}")
    @Operation(
            operationId = "getComment",
            summary = "Get a stored Reddit comment",
            description = "Returns one normalized comment, its thread relationship, collection metadata, "
                    + "and deletion state.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "The comment was found."),
            @APIResponse(responseCode = "404", description = "No comment exists with the supplied ID.")
    })
    public CommentResponse get(
            @Parameter(description = "Internal comment ID, not the Reddit base-36 ID.", example = "125", required = true)
            @PathParam("commentId") long commentId) {
        return commentService.get(commentId);
    }
}
