package com.example.reddit.reddit;

import com.example.reddit.config.CrawloraConfig;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class CrawloraRedditClient implements RedditClient {
    private static final Logger LOG = Logger.getLogger(CrawloraRedditClient.class);
    private static final int SEARCH_PAGE_LIMIT = 100;
    private static final long MAX_RETRY_DELAY_MS = 60_000L;

    private final CrawloraApi api;
    private final CrawloraResponseParser parser;
    private final CrawloraConfig config;

    @Inject
    public CrawloraRedditClient(
            @RestClient CrawloraApi api,
            CrawloraResponseParser parser,
            CrawloraConfig config) {
        this.api = api;
        this.parser = parser;
        this.config = config;
    }

    @Override
    public List<RedditPostData> searchPosts(
            String subreddit, String query, String sort, String timeRange, int maxPosts) {
        String apiKey = requiredApiKey();
        Map<String, RedditPostData> uniquePosts = new LinkedHashMap<>();
        Set<String> seenCursors = new HashSet<>();
        String after = null;

        do {
            int limit = Math.min(SEARCH_PAGE_LIMIT, maxPosts - uniquePosts.size());
            String cursor = after;
            JsonNode json = execute(
                    () -> api.search(query, subreddit, sort, timeRange, limit, cursor, apiKey),
                    "search subreddit=" + subreddit);
            CrawloraSearchPage page = parser.parseSearch(json);
            int previousSize = uniquePosts.size();
            page.posts().forEach(post -> uniquePosts.putIfAbsent(post.redditId(), post));
            after = page.after();
            if (uniquePosts.size() == previousSize) {
                LOG.warnf("Stopping Crawlora pagination after a page added no posts subreddit=%s",
                        subreddit);
                break;
            }
            if (after != null && !seenCursors.add(after)) {
                LOG.warnf("Stopping Crawlora pagination after repeated cursor subreddit=%s cursor=%s",
                        subreddit, after);
                break;
            }
        } while (after != null && uniquePosts.size() < maxPosts);

        List<RedditPostData> result = new ArrayList<>(uniquePosts.values());
        return result.size() <= maxPosts
                ? List.copyOf(result)
                : List.copyOf(result.subList(0, maxPosts));
    }

    @Override
    public RedditCommentThread fetchComments(String postRedditId) {
        String apiKey = requiredApiKey();
        int limit = config.commentsLimit();
        if (limit < 1 || limit > 100) {
            throw new RedditClientException("crawlora.comments-limit must be between 1 and 100");
        }
        JsonNode json = execute(
                () -> api.comments(postRedditId, "confidence", limit, apiKey),
                "comments postId=" + postRedditId);
        return parser.parseComments(json, limit);
    }

    private String requiredApiKey() {
        return config.apiKey()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .orElseThrow(() -> new RedditClientException(
                        "Crawlora API key is not configured; set CRAWLORA_API_KEY"));
    }

    private JsonNode execute(ResponseCall call, String operation) {
        int maxRetries = Math.max(0, config.maxRetries());
        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            try (Response response = call.invoke()) {
                int status = response.getStatus();
                if (status >= 200 && status < 300) {
                    return response.readEntity(JsonNode.class);
                }
                boolean retryable = status == 429 || status >= 500;
                LOG.warnf("Crawlora HTTP failure operation=\"%s\" status=%d attempt=%d retryable=%s",
                        operation, status, attempt + 1, retryable);
                if (!retryable || attempt == maxRetries) {
                    throw new RedditClientException(
                            "Crawlora request failed for " + operation + " with HTTP " + status);
                }
                long delay = retryDelay(response, status, attempt);
                LOG.warnf("Crawlora retry scheduled operation=\"%s\" status=%d retryDelayMs=%d",
                        operation, status, delay);
                sleep(delay, operation);
            } catch (ProcessingException exception) {
                LOG.warnf(exception, "Crawlora transport failure operation=\"%s\" attempt=%d",
                        operation, attempt + 1);
                if (attempt == maxRetries) {
                    throw new RedditClientException("Crawlora request failed for " + operation, exception);
                }
                sleep(backoff(attempt), operation);
            }
        }
        throw new RedditClientException("Crawlora request failed for " + operation);
    }

    private long retryDelay(Response response, int status, int attempt) {
        String header = response.getHeaderString("Retry-After");
        if (header == null) {
            return status == 429
                    ? Math.min(Math.max(0L, config.rateLimitRetryDelay()), MAX_RETRY_DELAY_MS)
                    : backoff(attempt);
        }
        try {
            return Math.min(Math.max(0L, Long.parseLong(header)) * 1_000L, MAX_RETRY_DELAY_MS);
        } catch (NumberFormatException ignored) {
            return backoff(attempt);
        }
    }

    private long backoff(int attempt) {
        long base = Math.max(0L, config.retryDelay());
        int shift = Math.min(attempt, 16);
        if (base > (MAX_RETRY_DELAY_MS >> shift)) {
            return MAX_RETRY_DELAY_MS;
        }
        return Math.min(base << shift, MAX_RETRY_DELAY_MS);
    }

    private static void sleep(long milliseconds, String operation) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RedditClientException(
                    "Interrupted while waiting to retry Crawlora " + operation, exception);
        }
    }

    @FunctionalInterface
    private interface ResponseCall {
        Response invoke();
    }
}
