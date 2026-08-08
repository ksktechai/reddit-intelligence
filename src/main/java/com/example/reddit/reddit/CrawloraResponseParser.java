package com.example.reddit.reddit;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class CrawloraResponseParser {
    private static final Logger LOG = Logger.getLogger(CrawloraResponseParser.class);

    public CrawloraSearchPage parseSearch(JsonNode envelope) {
        JsonNode data = data(envelope, "search");
        List<RedditPostData> posts = new ArrayList<>();
        JsonNode items = data.path("posts");
        if (items.isArray()) {
            for (JsonNode item : items) {
                String id = text(item, "id");
                if (id == null) {
                    LOG.warn("Ignoring Crawlora Reddit post without an id");
                    continue;
                }
                String url = text(item, "url");
                String permalink = text(item, "permalink");
                posts.add(new RedditPostData(
                        id,
                        textOrEmpty(item, "subreddit"),
                        textOrEmpty(item, "title"),
                        textOrEmpty(item, "selftext"),
                        author(item),
                        nonNegativeInt(item, "score"),
                        permalink == null ? url : permalink,
                        url,
                        instant(item),
                        nonNegativeInt(item, "comment_count")));
            }
        }
        return new CrawloraSearchPage(posts, text(data.path("pagination"), "after"));
    }

    public RedditCommentThread parseComments(JsonNode envelope, int requestedLimit) {
        JsonNode data = data(envelope, "comments");
        Map<String, FlatComment> comments = new LinkedHashMap<>();
        JsonNode items = data.path("comments");
        if (items.isArray()) {
            for (JsonNode item : items) {
                String id = text(item, "id");
                if (id == null) {
                    LOG.warn("Ignoring Crawlora Reddit comment without an id");
                    continue;
                }
                comments.putIfAbsent(id, new FlatComment(id, normalizeParent(text(item, "parent_id")), item));
            }
        }

        Map<String, List<String>> children = new HashMap<>();
        Set<String> attachedChildren = new HashSet<>();
        for (FlatComment comment : comments.values()) {
            String parentId = comment.parentId();
            if (parentId != null
                    && comments.containsKey(parentId)
                    && !createsCycle(comment.id(), parentId, comments)) {
                children.computeIfAbsent(parentId, ignored -> new ArrayList<>()).add(comment.id());
                attachedChildren.add(comment.id());
            }
        }

        List<RedditCommentData> roots = new ArrayList<>();
        for (FlatComment comment : comments.values()) {
            if (!attachedChildren.contains(comment.id())) {
                roots.add(toComment(comment.id(), 0, comments, children));
            }
        }
        boolean complete = comments.size() < requestedLimit;
        return new RedditCommentThread(roots, complete, 0);
    }

    private static RedditCommentData toComment(
            String id,
            int depth,
            Map<String, FlatComment> comments,
            Map<String, List<String>> children) {
        JsonNode item = comments.get(id).node();
        List<RedditCommentData> replies = children.getOrDefault(id, List.of()).stream()
                .map(childId -> toComment(childId, depth + 1, comments, children))
                .toList();
        String author = author(item);
        String body = text(item, "body");
        boolean deleted = author == null
                || "[deleted]".equals(author)
                || "[deleted]".equals(body)
                || "[removed]".equals(body);
        return new RedditCommentData(
                id,
                author,
                body,
                item.path("score").asInt(0),
                depth,
                instant(item),
                deleted,
                replies);
    }

    private static boolean createsCycle(
            String childId,
            String parentId,
            Map<String, FlatComment> comments) {
        Set<String> visited = new HashSet<>();
        String current = parentId;
        while (current != null && comments.containsKey(current) && visited.add(current)) {
            if (childId.equals(current)) {
                return true;
            }
            current = comments.get(current).parentId();
        }
        return false;
    }

    private static JsonNode data(JsonNode envelope, String operation) {
        if (envelope == null || !envelope.isObject()) {
            throw new RedditClientException("Crawlora returned an invalid " + operation + " response");
        }
        int code = envelope.path("code").asInt(200);
        JsonNode data = envelope.path("data");
        if (code < 200 || code >= 300 || !data.isObject()) {
            throw new RedditClientException("Crawlora returned an invalid " + operation
                    + " response with code " + code);
        }
        return data;
    }

    private static String author(JsonNode node) {
        JsonNode author = node.path("author");
        return author.isTextual() ? clean(author.asText()) : text(author, "name");
    }

    private static int nonNegativeInt(JsonNode node, String field) {
        return Math.max(0, node.path(field).asInt(0));
    }

    private static String normalizeParent(String parentId) {
        if (parentId == null || parentId.startsWith("t3_")) {
            return null;
        }
        return parentId.startsWith("t1_") ? clean(parentId.substring(3)) : parentId;
    }

    private static String textOrEmpty(JsonNode node, String field) {
        String value = text(node, field);
        return value == null ? "" : value;
    }

    private static String text(JsonNode node, String field) {
        if (node == null) {
            return null;
        }
        JsonNode value = node.get(field);
        return value == null || value.isNull() ? null : clean(value.asText());
    }

    private static String clean(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private static Instant instant(JsonNode node) {
        JsonNode epoch = node.get("created_utc");
        if (epoch != null && epoch.isNumber()) {
            return Instant.ofEpochSecond(epoch.asLong());
        }
        String created = text(node, "created");
        if (created == null) {
            return null;
        }
        try {
            return Instant.parse(created);
        } catch (DateTimeParseException exception) {
            LOG.warnf("Ignoring invalid Crawlora Reddit timestamp value=%s", created);
            return null;
        }
    }

    private record FlatComment(String id, String parentId, JsonNode node) {
    }
}
