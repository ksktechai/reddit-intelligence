package com.example.reddit.api;

import com.example.reddit.comment.CommentService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

@Path("/api/comments")
@Produces(MediaType.APPLICATION_JSON)
public class CommentResource {
    private final CommentService commentService;

    @Inject
    public CommentResource(CommentService commentService) {
        this.commentService = commentService;
    }

    @GET
    @Path("/{commentId}")
    public CommentResponse get(@PathParam("commentId") long commentId) {
        return commentService.get(commentId);
    }
}
