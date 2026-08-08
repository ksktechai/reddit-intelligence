package com.example.reddit.reddit;

import com.example.reddit.api.CreateDatasetRequest;
import com.example.reddit.dataset.ImportStatistics;
import com.example.reddit.persistence.ImportPersistenceService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class RedditImportService {
    private static final Logger LOG = Logger.getLogger(RedditImportService.class);

    private final RedditClient redditClient;
    private final ImportPersistenceService persistence;

    @Inject
    public RedditImportService(RedditClient redditClient, ImportPersistenceService persistence) {
        this.redditClient = redditClient;
        this.persistence = persistence;
    }

    public ImportStatistics importDataset(long datasetId, CreateDatasetRequest request) {
        LOG.infof("Starting Reddit import datasetId=%d subreddit=%s query=\"%s\"",
                datasetId, request.subreddit(), request.query());
        Instant createdAtOrAfter = request.fromDate() == null
                ? null
                : request.fromDate().atStartOfDay(ZoneOffset.UTC).toInstant();
        List<RedditPostData> searchResults = redditClient.searchPosts(
                request.subreddit(), request.query(), request.sort(),
                request.timeRange(), createdAtOrAfter, request.maxPosts());
        List<RedditPostData> posts = deduplicate(searchResults).stream()
                .filter(post -> isOnOrAfter(post, createdAtOrAfter))
                .toList();
        LOG.infof("Reddit posts fetched datasetId=%d subreddit=%s postsFetched=%d",
                datasetId, request.subreddit(), posts.size());

        int commentsImported = 0;
        for (RedditPostData post : posts) {
            Instant collectedAt = Instant.now();
            long postId = persistence.upsertPost(datasetId, post, collectedAt);
            if (request.includeComments()) {
                try {
                    RedditCommentThread thread = redditClient.fetchComments(post.redditId());
                    commentsImported += persistence.upsertComments(postId, thread, collectedAt);
                    LOG.infof("Reddit comments fetched datasetId=%d postId=%s commentsFetched=%d complete=%s moreObjects=%d",
                            datasetId, post.redditId(), thread.totalComments(),
                            thread.complete(), thread.moreObjects());
                } catch (RedditClientException exception) {
                    // Retain the post and leave comments_downloaded=false. A single inaccessible
                    // thread should not discard other successfully collected research data.
                    LOG.errorf(exception,
                            "Reddit comments fetch failed datasetId=%d postId=%s", datasetId, post.redditId());
                }
            }
        }

        LOG.infof("Completed Reddit import datasetId=%d subreddit=%s postsImported=%d commentsImported=%d",
                datasetId, request.subreddit(), posts.size(), commentsImported);
        return new ImportStatistics(posts.size(), commentsImported);
    }

    static List<RedditPostData> deduplicate(List<RedditPostData> posts) {
        Map<String, RedditPostData> unique = new LinkedHashMap<>();
        for (RedditPostData post : posts) {
            unique.putIfAbsent(post.redditId(), post);
        }
        return List.copyOf(unique.values());
    }

    private static boolean isOnOrAfter(RedditPostData post, Instant createdAtOrAfter) {
        return createdAtOrAfter == null
                || post.createdAt() != null && !post.createdAt().isBefore(createdAtOrAfter);
    }
}
