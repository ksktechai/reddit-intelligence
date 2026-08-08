package com.example.reddit.comment;

import com.example.reddit.api.CommentResponse;
import com.example.reddit.persistence.RedditCommentEntity;
import com.example.reddit.persistence.RedditPostEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;

@ApplicationScoped
public class CommentService {

    @Transactional
    public CommentResponse get(long id) {
        RedditCommentEntity comment = RedditCommentEntity.findById(id);
        if (comment == null) {
            throw new NotFoundException("Comment not found: " + id);
        }
        return toResponse(comment);
    }

    @Transactional
    public List<CommentResponse> forPost(long postId) {
        if (RedditPostEntity.findById(postId) == null) {
            throw new NotFoundException("Post not found: " + postId);
        }
        return RedditCommentEntity.<RedditCommentEntity>find(
                        "post.id = ?1 order by depth asc, createdAt asc, id asc", postId)
                .list()
                .stream()
                .map(CommentService::toResponse)
                .toList();
    }

    private static CommentResponse toResponse(RedditCommentEntity comment) {
        return new CommentResponse(
                comment.id,
                comment.redditId,
                comment.post.id,
                comment.parentComment == null ? null : comment.parentComment.id,
                comment.author,
                comment.body,
                comment.score,
                comment.depth,
                comment.createdAt,
                comment.collectedAt,
                comment.deleted);
    }
}
