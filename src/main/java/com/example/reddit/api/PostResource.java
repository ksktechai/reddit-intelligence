package com.example.reddit.api;

import com.example.reddit.comment.CommentService;
import com.example.reddit.post.PostService;
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

import java.util.List;

@Path("/api/posts")
@Produces(MediaType.APPLICATION_JSON)
@Tag(name = "Reddit Content", description = "Inspect Reddit posts and comments already stored in the database.")
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
    @Operation(
            operationId = "getPost",
            summary = "Get a stored Reddit post",
            description = "Returns the normalized post content and collection metadata for one internal post ID.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "The post was found."),
            @APIResponse(responseCode = "404", description = "No post exists with the supplied ID.")
    })
    public PostResponse get(
            @Parameter(description = "Internal post ID, not the Reddit base-36 ID.", example = "42", required = true)
            @PathParam("postId") long postId) {
        return postService.get(postId);
    }

    @GET
    @Path("/{postId}/comments")
    @Operation(
            operationId = "listPostComments",
            summary = "List comments for a stored post",
            description = "Returns comments previously imported for the post in thread order. This endpoint "
                    + "does not fetch or refresh comments from Crawlora.")
    @APIResponses({
            @APIResponse(responseCode = "200", description = "Stored comments were returned; the list may be empty."),
            @APIResponse(responseCode = "404", description = "No post exists with the supplied ID.")
    })
    public List<CommentResponse> comments(
            @Parameter(description = "Internal post ID, not the Reddit base-36 ID.", example = "42", required = true)
            @PathParam("postId") long postId) {
        return commentService.forPost(postId);
    }
}
