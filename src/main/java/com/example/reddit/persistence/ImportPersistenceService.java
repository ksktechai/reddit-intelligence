package com.example.reddit.persistence;

import com.example.reddit.reddit.RedditCommentData;
import com.example.reddit.reddit.RedditCommentThread;
import com.example.reddit.reddit.RedditPostData;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;

@ApplicationScoped
public class ImportPersistenceService {

    @Transactional
    public long upsertPost(long datasetId, RedditPostData post, Instant collectedAt) {
        DatasetEntity dataset = DatasetEntity.findById(datasetId);
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset not found: " + datasetId);
        }

        RedditPostEntity entity = RedditPostEntity.find("redditId", post.redditId()).firstResult();
        if (entity == null) {
            entity = new RedditPostEntity();
            entity.redditId = post.redditId();
            entity.commentsDownloaded = false;
            entity.commentsComplete = false;
        }
        copyPost(entity, post, collectedAt);
        if (!entity.isPersistent()) {
            entity.persistAndFlush();
        }
        dataset.posts.add(entity);
        return entity.id;
    }

    @Transactional
    public int upsertComments(long postId, RedditCommentThread thread, Instant collectedAt) {
        RedditPostEntity post = RedditPostEntity.findById(postId);
        if (post == null) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }

        for (RedditCommentData comment : thread.comments()) {
            upsertComment(post, null, comment, collectedAt);
        }
        post.commentsDownloaded = true;
        post.commentsComplete = thread.complete()
                && thread.totalComments() >= post.commentCountReported;
        RedditCommentEntity.flush();
        return thread.totalComments();
    }

    private void upsertComment(
            RedditPostEntity post,
            RedditCommentEntity parent,
            RedditCommentData comment,
            Instant collectedAt) {
        RedditCommentEntity entity = RedditCommentEntity.find("redditId", comment.redditId()).firstResult();
        if (entity == null) {
            entity = new RedditCommentEntity();
            entity.redditId = comment.redditId();
            entity.post = post;
        } else if (!entity.post.id.equals(post.id)) {
            throw new IllegalStateException("Reddit comment belongs to a different post: " + comment.redditId());
        }

        entity.parentComment = parent;
        entity.author = comment.author();
        entity.body = comment.body();
        entity.score = comment.score();
        entity.depth = comment.depth();
        entity.createdAt = comment.createdAt();
        entity.collectedAt = collectedAt;
        entity.deleted = comment.deleted();
        if (!entity.isPersistent()) {
            entity.persistAndFlush();
        }

        for (RedditCommentData reply : comment.replies()) {
            upsertComment(post, entity, reply, collectedAt);
        }
    }

    private static void copyPost(RedditPostEntity entity, RedditPostData post, Instant collectedAt) {
        entity.subreddit = post.subreddit();
        entity.title = post.title();
        entity.body = post.body();
        entity.author = post.author();
        entity.score = post.score();
        entity.permalink = post.permalink();
        entity.externalUrl = post.externalUrl();
        entity.createdAt = post.createdAt();
        entity.collectedAt = collectedAt;
        entity.commentCountReported = post.commentCountReported();
    }
}
