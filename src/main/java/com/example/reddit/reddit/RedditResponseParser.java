package com.example.reddit.reddit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class RedditResponseParser {
    private static final Logger LOG = Logger.getLogger(RedditResponseParser.class);

    public RedditSearchPage parseSearch(JsonNode root) {
        JsonNode data = root.path("data");
        List<RedditPostData> posts = new ArrayList<>();
        for (JsonNode child : data.path("children")) {
            String kind = child.path("kind").asText();
            if (!"t3".equals(kind)) {
                LOG.debugf("Ignoring unsupported search object kind=%s", kind);
                continue;
            }
            JsonNode post = child.path("data");
            String id = text(post, "id");
            if (id == null) {
                LOG.warn("Ignoring Reddit post without an id");
                continue;
            }
            posts.add(new RedditPostData(
                    id,
                    textOrEmpty(post, "subreddit"),
                    textOrEmpty(post, "title"),
                    textOrEmpty(post, "selftext"),
                    text(post, "author"),
                    post.path("score").asInt(0),
                    text(post, "permalink"),
                    text(post, "url"),
                    instant(post, "created_utc"),
                    Math.max(0, post.path("num_comments").asInt(0))));
        }
        return new RedditSearchPage(posts, text(data, "after"));
    }

    public RedditCommentThread parseComments(JsonNode root) {
        if (!root.isArray() || root.size() < 2) {
            throw new RedditClientException("Unexpected Reddit comments response shape");
        }

        ParseState state = new ParseState();
        List<RedditCommentData> comments = parseChildren(
                root.get(1).path("data").path("children"), 0, state);
        if (state.moreObjects > 0) {
            LOG.debugf("Reddit response contained more placeholders count=%d", state.moreObjects);
        }
        return new RedditCommentThread(comments, state.moreObjects == 0, state.moreObjects);
    }

    private List<RedditCommentData> parseChildren(JsonNode children, int depth, ParseState state) {
        List<RedditCommentData> comments = new ArrayList<>();
        if (!children.isArray()) {
            return comments;
        }

        for (JsonNode child : children) {
            String kind = child.path("kind").asText();
            if ("more".equals(kind)) {
                state.moreObjects++;
                continue;
            }
            if (!"t1".equals(kind)) {
                LOG.debugf("Ignoring unsupported comment object kind=%s", kind);
                continue;
            }

            JsonNode data = child.path("data");
            String id = text(data, "id");
            if (id == null) {
                LOG.warn("Ignoring Reddit comment without an id");
                continue;
            }

            String author = text(data, "author");
            String body = text(data, "body");
            boolean deleted = author == null
                    || "[deleted]".equals(author)
                    || "[deleted]".equals(body)
                    || "[removed]".equals(body);
            List<RedditCommentData> replies = parseReplies(data.path("replies"), depth + 1, state);
            comments.add(new RedditCommentData(
                    id,
                    author,
                    body,
                    data.path("score").asInt(0),
                    depth,
                    instant(data, "created_utc"),
                    deleted,
                    replies));
        }
        return comments;
    }

    private List<RedditCommentData> parseReplies(JsonNode replies, int depth, ParseState state) {
        if (!replies.isObject()) {
            return List.of();
        }
        return parseChildren(replies.path("data").path("children"), depth, state);
    }

    private static String textOrEmpty(JsonNode node, String field) {
        String value = text(node, field);
        return value == null ? "" : value;
    }

    private static String text(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || value.isNull() || value.isMissingNode()) {
            return null;
        }
        String text = value.asText();
        return text.isBlank() ? null : text;
    }

    private static Instant instant(JsonNode node, String field) {
        JsonNode value = node.get(field);
        if (value == null || !value.isNumber()) {
            return null;
        }
        return Instant.ofEpochSecond(value.asLong());
    }

    private static final class ParseState {
        private int moreObjects;
    }
}
