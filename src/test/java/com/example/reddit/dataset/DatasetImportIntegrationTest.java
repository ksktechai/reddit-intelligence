package com.example.reddit.dataset;

import com.example.reddit.persistence.DatasetEntity;
import com.example.reddit.persistence.RedditCommentEntity;
import com.example.reddit.persistence.RedditPostEntity;
import com.example.reddit.reddit.RedditClient;
import com.example.reddit.reddit.RedditCommentData;
import com.example.reddit.reddit.RedditCommentThread;
import com.example.reddit.reddit.RedditPostData;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;

@QuarkusTest
class DatasetImportIntegrationTest {

    @InjectMock
    RedditClient redditClient;

    @Inject
    EntityManager entityManager;

    @Test
    @Transactional
    void createsDatasetPersistsHierarchyAndDeduplicatesRepeatedImport() {
        RedditPostData post = new RedditPostData(
                "phase1post", "universityofauckland", "Master of AI",
                "What is the programme like?", "prospective_student", 15,
                "/r/universityofauckland/comments/phase1post/master_of_ai/",
                "https://www.reddit.com/r/universityofauckland/comments/phase1post/master_of_ai/",
                Instant.parse("2026-02-01T00:00:00Z"), 2);
        RedditCommentData reply = new RedditCommentData(
                "phase1reply", null, "[deleted]", 0, 1,
                Instant.parse("2026-02-01T01:05:00Z"), true, List.of());
        RedditCommentData root = new RedditCommentData(
                "phase1root", "current_student", "It has strong applied content.",
                6, 0, Instant.parse("2026-02-01T01:00:00Z"), false, List.of(reply));

        when(redditClient.searchPosts(any(), any(), any(), any(), anyInt()))
                .thenReturn(List.of(post));
        when(redditClient.fetchComments("phase1post"))
                .thenReturn(new RedditCommentThread(List.of(root), true, 0));

        long firstDatasetId = createDataset();

        long postId = given()
                .when().get("/api/datasets/{id}/posts", firstDatasetId)
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].redditId", equalTo("phase1post"))
                .body("[0].commentsDownloaded", equalTo(true))
                .body("[0].commentsComplete", equalTo(true))
                .extract().jsonPath().getLong("[0].id");

        List<Integer> commentIds = given()
                .when().get("/api/posts/{id}/comments", postId)
                .then().statusCode(200)
                .body("$", hasSize(2))
                .body("[0].depth", equalTo(0))
                .body("[1].depth", equalTo(1))
                .body("[1].deleted", equalTo(true))
                .extract().jsonPath().getList("id");
        int parentId = given()
                .when().get("/api/posts/{id}/comments", postId)
                .then().extract().jsonPath().getInt("[1].parentCommentId");
        assertEquals(commentIds.getFirst().intValue(), parentId);

        long secondDatasetId = createDataset();
        given().when().get("/api/datasets/{id}", secondDatasetId)
                .then().statusCode(200)
                .body("postsImported", equalTo(1))
                .body("commentsImported", equalTo(2))
                .body("status", equalTo("COMPLETED"));

        // The second dataset links to the existing raw records instead of duplicating them.
        assertEquals(2, DatasetEntity.count());
        assertEquals(1, RedditPostEntity.count());
        assertEquals(2, RedditCommentEntity.count());
        Number datasetPostCount = (Number) entityManager
                .createNativeQuery("select count(*) from dataset_post")
                .getSingleResult();
        assertEquals(2L, datasetPostCount.longValue());
    }

    private static long createDataset() {
        return given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "subreddit": "universityofauckland",
                          "query": "Master of Artificial Intelligence",
                          "sort": "relevance",
                          "timeRange": "all",
                          "maxPosts": 100,
                          "includeComments": true
                        }
                        """)
                .when().post("/api/datasets")
                .then().statusCode(201)
                .body("subreddit", equalTo("universityofauckland"))
                .body("postsImported", equalTo(1))
                .body("commentsImported", equalTo(2))
                .body("status", equalTo("COMPLETED"))
                .extract().jsonPath().getLong("datasetId");
    }
}
