package com.example.reddit.reddit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CrawloraResponseParserTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final CrawloraResponseParser parser = new CrawloraResponseParser();

    @Test
    void parsesSearchPostsPaginationAndOptionalFields() throws IOException {
        CrawloraSearchPage page = parser.parseSearch(fixture("crawlora-search.json"));

        assertEquals("t3_next", page.after());
        assertEquals(2, page.posts().size());
        RedditPostData first = page.posts().getFirst();
        assertEquals("post1", first.redditId());
        assertEquals("Master of Artificial Intelligence experience", first.title());
        assertEquals("student_one", first.author());
        assertEquals(42, first.score());
        assertEquals(3, first.commentCountReported());
        assertEquals(Instant.parse("2024-03-09T16:00:00Z"), first.createdAt());
        assertEquals("https://www.reddit.com/r/universityofauckland/comments/post1/example/",
                first.permalink());

        RedditPostData missingFields = page.posts().get(1);
        assertNull(missingFields.author());
        assertEquals("", missingFields.body());
        assertEquals(0, missingFields.score());
        assertEquals(0, missingFields.commentCountReported());
        assertEquals(Instant.parse("2024-03-09T16:01:40Z"), missingFields.createdAt());
    }

    @Test
    void rebuildsHierarchyFromFlatCommentsAndMarksDeletedEntries() throws IOException {
        RedditCommentThread thread = parser.parseComments(fixture("crawlora-comments.json"), 100);

        assertTrue(thread.complete());
        assertEquals(4, thread.totalComments());
        assertEquals(2, thread.comments().size());

        RedditCommentData root = thread.comments().getFirst();
        assertEquals("comment1", root.redditId());
        assertEquals(0, root.depth());
        assertEquals(2, root.replies().size());
        RedditCommentData reply = root.replies().getFirst();
        assertEquals("comment2", reply.redditId());
        assertEquals(1, reply.depth());
        assertEquals(4, reply.score());

        RedditCommentData deletedReply = root.replies().get(1);
        assertTrue(deletedReply.deleted());
        assertNull(deletedReply.author());
        assertEquals("[deleted]", deletedReply.body());

        RedditCommentData orphan = thread.comments().get(1);
        assertEquals("orphan", orphan.redditId());
        assertEquals(0, orphan.depth());
    }

    @Test
    void treatsAFullCommentPageAsPotentiallyIncomplete() throws IOException {
        RedditCommentThread thread = parser.parseComments(fixture("crawlora-comments.json"), 4);

        assertFalse(thread.complete());
    }

    @Test
    void rejectsErrorEnvelope() throws IOException {
        JsonNode response = objectMapper.readTree("""
                {"code": 429, "msg": "rate limited", "data": null}
                """);

        RedditClientException exception = assertThrows(
                RedditClientException.class,
                () -> parser.parseSearch(response));
        assertTrue(exception.getMessage().contains("code 429"));
    }

    private JsonNode fixture(String name) throws IOException {
        try (InputStream stream = getClass().getResourceAsStream("/reddit/" + name)) {
            if (stream == null) {
                throw new IOException("Missing fixture: " + name);
            }
            return objectMapper.readTree(stream);
        }
    }
}
