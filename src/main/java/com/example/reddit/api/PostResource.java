package com.example.reddit.api;

import com.example.reddit.comment.CommentService;
import com.example.reddit.post.PostService;
import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import java.util.List;

@Path("/api/posts")
@Produces(MediaType.APPLICATION_JSON)
public class PostResource {
    private final PostService postService;
    private final CommentService commentService;

    @Inject
    public PostResource(PostService postService, CommentService commentService) {
        this.postService = postService;
        this.commentService = commentService;
    }

    @GET
    @Path("/{postId}")
    public PostResponse get(@PathParam("postId") long postId) {
        return postService.get(postId);
    }

    @GET
    @Path("/{postId}/comments")
    public List<CommentResponse> comments(@PathParam("postId") long postId) {
        return commentService.forPost(postId);
    }
}
