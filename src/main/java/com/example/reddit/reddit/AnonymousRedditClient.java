package com.example.reddit.reddit;

import com.example.reddit.config.RedditConfig;
import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;

@ApplicationScoped
public class AnonymousRedditClient implements RedditClient {
    private static final Logger LOG = Logger.getLogger(AnonymousRedditClient.class);
    private static final int SEARCH_PAGE_LIMIT = 100;

    private final RedditJsonApi api;
    private final RedditResponseParser parser;
    private final RedditConfig config;

    @Inject
    public AnonymousRedditClient(
            @RestClient RedditJsonApi api,
            RedditResponseParser parser,
            RedditConfig config) {
        this.api = api;
        this.parser = parser;
        this.config = config;
    }

    @Override
    public List<RedditPostData> searchPosts(
            String subreddit, String query, String sort, String timeRange, int maxPosts) {
        Map<String, RedditPostData> uniquePosts = new LinkedHashMap<>();
        Set<String> seenCursors = new HashSet<>();
        String after = null;

        do {
            int limit = Math.min(SEARCH_PAGE_LIMIT, maxPosts - uniquePosts.size());
            String cursor = after;
            JsonNode json = execute(
                    () -> api.search(subreddit, query, "on", sort, timeRange,
                            limit, cursor, 1, config.userAgent()),
                    "search subreddit=" + subreddit);
            RedditSearchPage page = parser.parseSearch(json);
            page.posts().forEach(post -> uniquePosts.putIfAbsent(post.redditId(), post));
            after = page.after();
            if (after != null && !seenCursors.add(after)) {
                LOG.warnf("Stopping Reddit pagination after repeated cursor subreddit=%s cursor=%s",
                        subreddit, after);
                break;
            }
        } while (after != null && uniquePosts.size() < maxPosts);

        List<RedditPostData> result = new ArrayList<>(uniquePosts.values());
        return result.size() <= maxPosts ? List.copyOf(result) : List.copyOf(result.subList(0, maxPosts));
    }

    @Override
    public RedditCommentThread fetchComments(String postRedditId) {
        JsonNode json = execute(
                () -> api.comments(postRedditId, 500, 1, config.userAgent()),
                "comments postId=" + postRedditId);
        return parser.parseComments(json);
    }

    private JsonNode execute(ResponseCall call, String operation) {
        for (int attempt = 0; attempt <= config.maxRetries(); attempt++) {
            try (Response response = call.invoke()) {
                int status = response.getStatus();
                if (status >= 200 && status < 300) {
                    return response.readEntity(JsonNode.class);
                }
                boolean retryable = status == 429 || status >= 500;
                LOG.warnf("Reddit HTTP failure operation=\"%s\" status=%d attempt=%d retryable=%s",
                        operation, status, attempt + 1, retryable);
                if (!retryable || attempt == config.maxRetries()) {
                    throw new RedditClientException(
                            "Reddit request failed for " + operation + " with HTTP " + status);
                }
                long delay = retryDelay(response);
                if (status == 429) {
                    LOG.warnf("Reddit rate limiting operation=\"%s\" retryDelayMs=%d", operation, delay);
                }
                sleep(delay);
            } catch (ProcessingException exception) {
                LOG.warnf(exception, "Reddit transport failure operation=\"%s\" attempt=%d",
                        operation, attempt + 1);
                if (attempt == config.maxRetries()) {
                    throw new RedditClientException("Reddit request failed for " + operation, exception);
                }
                sleep(config.retryDelay());
            }
        }
        throw new RedditClientException("Reddit request failed for " + operation);
    }

    private long retryDelay(Response response) {
        String header = response.getHeaderString("Retry-After");
        if (header == null) {
            return config.retryDelay();
        }
        try {
            return Math.min(Long.parseLong(header) * 1_000L, 60_000L);
        } catch (NumberFormatException ignored) {
            return config.retryDelay();
        }
    }

    private static void sleep(long milliseconds) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new RedditClientException("Interrupted while waiting to retry Reddit", exception);
        }
    }

    @FunctionalInterface
    private interface ResponseCall {
        Response invoke();
    }
}
