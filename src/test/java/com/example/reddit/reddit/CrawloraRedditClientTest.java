package com.example.reddit.reddit;

import com.example.reddit.config.CrawloraConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrawloraRedditClientTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private CrawloraApi api;
    private CrawloraConfig config;
    private CrawloraRedditClient client;

    @BeforeEach
    void setUp() {
        api = mock(CrawloraApi.class);
        config = mock(CrawloraConfig.class);
        when(config.apiKey()).thenReturn(Optional.of("test-key"));
        when(config.commentsLimit()).thenReturn(100);
        when(config.maxRetries()).thenReturn(2);
        when(config.retryDelay()).thenReturn(0L);
        when(config.rateLimitRetryDelay()).thenReturn(0L);
        client = new CrawloraRedditClient(api, new CrawloraResponseParser(), config);
    }

    @Test
    void followsSearchCursorAndUsesOnlyTheRequestedNumberOfPosts() throws Exception {
        JsonNode firstPage = objectMapper.readTree("""
                {
                  "code": 200,
                  "data": {
                    "posts": [{"id":"first","subreddit":"java","title":"First"}],
                    "pagination": {"after":"t3_first"}
                  }
                }
                """);
        JsonNode secondPage = objectMapper.readTree("""
                {
                  "code": 200,
                  "data": {
                    "posts": [
                      {"id":"second","subreddit":"java","title":"Second"},
                      {"id":"third","subreddit":"java","title":"Third"}
                    ],
                    "pagination": {"after":"t3_third"}
                  }
                }
                """);
        Response firstResponse = response(200, firstPage);
        Response secondResponse = response(200, secondPage);
        when(api.search("quarkus", "java", "relevance", "all", 3, null, "test-key"))
                .thenReturn(firstResponse);
        when(api.search("quarkus", "java", "relevance", "all", 2, "t3_first", "test-key"))
                .thenReturn(secondResponse);

        List<RedditPostData> posts = client.searchPosts("java", "quarkus", "relevance", "all", 3);

        assertEquals(List.of("first", "second", "third"),
                posts.stream().map(RedditPostData::redditId).toList());
        verify(api).search("quarkus", "java", "relevance", "all", 3, null, "test-key");
        verify(api).search("quarkus", "java", "relevance", "all", 2, "t3_first", "test-key");
    }

    @Test
    void retriesTemporaryHttpAndTransportFailures() throws Exception {
        JsonNode success = objectMapper.readTree("""
                {"code":200,"data":{"posts":[{"id":"post1","subreddit":"java","title":"Post"}]}}
                """);
        Response unavailable = response(503, null);
        Response successfulResponse = response(200, success);
        when(unavailable.getHeaderString("Retry-After")).thenReturn("0");
        when(api.search("quarkus", "java", "new", "week", 1, null, "test-key"))
                .thenThrow(new ProcessingException("temporary network failure"))
                .thenReturn(unavailable)
                .thenReturn(successfulResponse);

        List<RedditPostData> posts = client.searchPosts("java", "quarkus", "new", "week", 1);

        assertEquals(1, posts.size());
        assertEquals("post1", posts.getFirst().redditId());
    }

    @Test
    void fetchesCommentsWithConfiguredProviderLimit() throws Exception {
        JsonNode responseBody = objectMapper.readTree("""
                {
                  "code": 200,
                  "data": {
                    "comments": [
                      {"id":"comment1","parent_id":"t3_post1","body":"Answer",
                       "author":{"name":"student"}}
                    ]
                  }
                }
                """);
        Response successfulResponse = response(200, responseBody);
        when(config.commentsLimit()).thenReturn(75);
        when(api.comments("post1", "confidence", 75, "test-key"))
                .thenReturn(successfulResponse);

        RedditCommentThread thread = client.fetchComments("post1");

        assertTrue(thread.complete());
        assertEquals(1, thread.totalComments());
        verify(api).comments("post1", "confidence", 75, "test-key");
    }

    @Test
    void doesNotRetryClientErrors() {
        Response unauthorized = response(401, null);
        when(api.search("quarkus", "java", "new", "week", 1, null, "test-key"))
                .thenReturn(unauthorized);

        RedditClientException exception = assertThrows(
                RedditClientException.class,
                () -> client.searchPosts("java", "quarkus", "new", "week", 1));

        assertTrue(exception.getMessage().contains("HTTP 401"));
    }

    @Test
    void retriesRateLimitResponses() throws Exception {
        JsonNode success = objectMapper.readTree("""
                {"code":200,"data":{"posts":[{"id":"post1","subreddit":"java","title":"Post"}]}}
                """);
        Response rateLimited = response(429, null);
        Response successfulResponse = response(200, success);
        when(api.search("quarkus", "java", "new", "week", 1, null, "test-key"))
                .thenReturn(rateLimited)
                .thenReturn(successfulResponse);

        List<RedditPostData> posts = client.searchPosts("java", "quarkus", "new", "week", 1);

        assertEquals(1, posts.size());
        verify(api, org.mockito.Mockito.times(2))
                .search("quarkus", "java", "new", "week", 1, null, "test-key");
    }

    @Test
    void requiresServerSideApiKey() {
        when(config.apiKey()).thenReturn(Optional.empty());

        RedditClientException exception = assertThrows(
                RedditClientException.class,
                () -> client.searchPosts("java", "quarkus", "new", "week", 1));

        assertTrue(exception.getMessage().contains("CRAWLORA_API_KEY"));
    }

    private static Response response(int status, JsonNode body) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        if (body != null) {
            when(response.readEntity(JsonNode.class)).thenReturn(body);
        }
        return response;
    }
}
