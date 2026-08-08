package com.example.reddit.reddit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RedditResponseParserTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RedditResponseParser parser = new RedditResponseParser();

    @Test
    void parsesSearchPostsAndMissingFields() throws IOException {
        RedditSearchPage page = parser.parseSearch(fixture("search.json"));

        assertEquals("t3_next", page.after());
        assertEquals(2, page.posts().size());
        RedditPostData first = page.posts().getFirst();
        assertEquals("post1", first.redditId());
        assertEquals("Master of Artificial Intelligence experience", first.title());
        assertEquals(42, first.score());
        assertEquals(3, first.commentCountReported());

        RedditPostData missingFields = page.posts().get(1);
        assertNull(missingFields.author());
        assertEquals("", missingFields.body());
        assertEquals(0, missingFields.score());
        assertEquals(0, missingFields.commentCountReported());
    }

    @Test
    void recursivelyParsesNestedAndDeletedCommentsAndMarksMoreIncomplete() throws IOException {
        RedditCommentThread thread = parser.parseComments(fixture("comments.json"));

        assertFalse(thread.complete());
        assertEquals(2, thread.moreObjects());
        assertEquals(3, thread.totalComments());
        assertEquals(2, thread.comments().size());

        RedditCommentData root = thread.comments().getFirst();
        assertEquals(0, root.depth());
        assertEquals(1, root.replies().size());
        RedditCommentData deletedReply = root.replies().getFirst();
        assertEquals(1, deletedReply.depth());
        assertTrue(deletedReply.deleted());
        assertNull(deletedReply.author());
        assertEquals("[deleted]", deletedReply.body());
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
