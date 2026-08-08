package com.example.reddit.post;

import com.example.reddit.api.PostResponse;
import com.example.reddit.persistence.DatasetEntity;
import com.example.reddit.persistence.RedditPostEntity;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class PostService {

    @Transactional
    public PostResponse get(long id) {
        RedditPostEntity post = RedditPostEntity.findById(id);
        if (post == null) {
            throw new NotFoundException("Post not found: " + id);
        }
        return toResponse(post);
    }

    @Transactional
    public List<PostResponse> forDataset(long datasetId) {
        DatasetEntity dataset = DatasetEntity.findById(datasetId);
        if (dataset == null) {
            throw new NotFoundException("Dataset not found: " + datasetId);
        }
        return dataset.posts.stream()
                .sorted(Comparator.comparing(
                        (RedditPostEntity post) -> post.createdAt,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .map(PostService::toResponse)
                .toList();
    }

    private static PostResponse toResponse(RedditPostEntity post) {
        return new PostResponse(
                post.id,
                post.redditId,
                post.subreddit,
                post.title,
                post.body,
                post.author,
                post.score,
                post.permalink,
                post.externalUrl,
                post.createdAt,
                post.collectedAt,
                post.commentCountReported,
                post.commentsDownloaded,
                post.commentsComplete);
    }
}
