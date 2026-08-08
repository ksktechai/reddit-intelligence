package com.example.reddit.reddit;

import com.example.reddit.api.CreateDatasetRequest;
import com.example.reddit.dataset.ImportStatistics;
import com.example.reddit.persistence.ImportPersistenceService;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedditImportServiceTest {

    @Test
    void deduplicatesPostsBeforePersistence() {
        RedditClient client = mock(RedditClient.class);
        ImportPersistenceService persistence = mock(ImportPersistenceService.class);
        RedditPostData first = post("same-id", "First title");
        RedditPostData duplicate = post("same-id", "Duplicate title");
        RedditPostData second = post("other-id", "Other title");
        when(client.searchPosts(any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(first, duplicate, second));
        when(persistence.upsertPost(anyLong(), any(), any())).thenReturn(1L);

        RedditImportService service = new RedditImportService(client, persistence);
        CreateDatasetRequest request = new CreateDatasetRequest(
                "java", "quarkus", "relevance", "all", 10, false);
        ImportStatistics statistics = service.importDataset(7L, request);

        assertEquals(2, statistics.postsImported());
        verify(persistence, times(2)).upsertPost(anyLong(), any(), any());
    }

    private static RedditPostData post(String id, String title) {
        return new RedditPostData(
                id, "java", title, "body", "author", 1,
                "/r/java/comments/" + id, "https://reddit.com/" + id,
                Instant.parse("2026-01-01T00:00:00Z"), 0);
    }
}
