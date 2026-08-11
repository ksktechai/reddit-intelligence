package com.example.reddit.analysis;

import com.example.reddit.persistence.ClaimType;
import com.example.reddit.persistence.EvidenceStance;
import com.example.reddit.persistence.Sentiment;
import com.example.reddit.reddit.RedditClient;
import com.example.reddit.reddit.RedditCommentThread;
import com.example.reddit.reddit.RedditPostData;
import io.quarkus.test.InjectMock;
import io.quarkus.test.junit.QuarkusTest;
import io.restassured.http.ContentType;
import jakarta.inject.Inject;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static io.restassured.RestAssured.given;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

@QuarkusTest
class Phase2AnalysisIntegrationTest {

    @InjectMock
    RedditClient redditClient;

    @InjectMock
    AnalysisModel analysisModel;

    @InjectMock
    AnalysisTaskExecutor taskExecutor;

    @Inject
    AnalysisProcessor processor;

    @Test
    void createsRunsAndPersistsEvidenceBackedStructuredAnalysis() {
        RedditPostData post = new RedditPostData(
                "phase2post",
                "PersonalFinanceNZ",
                "Graduate salary",
                "The starting salary was 65k per year.",
                "graduate",
                8,
                "/r/PersonalFinanceNZ/comments/phase2post/graduate_salary/",
                "https://www.reddit.com/r/PersonalFinanceNZ/comments/phase2post/graduate_salary/",
                Instant.parse("2024-06-01T00:00:00Z"),
                0);
        when(redditClient.searchPosts(any(), any(), any(), any(), nullable(Instant.class), anyInt()))
                .thenReturn(List.of(post));
        when(redditClient.fetchComments(anyString()))
                .thenReturn(new RedditCommentThread(List.of(), true, 0));
        doNothing().when(taskExecutor).submit(any());

        long datasetId = given()
                .contentType(ContentType.JSON)
                .body("""
                        {
                          "subreddit":"PersonalFinanceNZ",
                          "query":"graduate salary phase2",
                          "fromDate":"2023-01-01",
                          "maxPosts":10,
                          "includeComments":false
                        }
                        """)
                .when().post("/api/datasets")
                .then().statusCode(201)
                .body("status", equalTo("COMPLETED"))
                .extract().jsonPath().getLong("datasetId");
        long postId = given()
                .when().get("/api/datasets/{datasetId}/posts", datasetId)
                .then().statusCode(200)
                .extract().jsonPath().getLong("[0].id");

        long analysisId = given()
                .contentType(ContentType.JSON)
                .body("{\"model\":\"test-model\"}")
                .when().post("/api/datasets/{datasetId}/analyses", datasetId)
                .then().statusCode(202)
                .header("Location", containsString("/api/analyses/"))
                .body("status", equalTo("PENDING"))
                .body("model", equalTo("test-model"))
                .extract().jsonPath().getLong("analysisId");

        ExtractedTopic topic = new ExtractedTopic(
                "Graduate pay",
                "Reported entry-level compensation",
                Sentiment.NEUTRAL,
                0.0,
                1);
        ExtractedEvidence evidence = new ExtractedEvidence(
                EvidenceSourceType.POST,
                postId,
                EvidenceStance.SUPPORTS,
                "The starting salary was 65k per year.",
                "The post directly reports the amount.");
        ExtractedClaim claim = new ExtractedClaim(
                "Graduate pay",
                "One participant reported a 65k starting salary.",
                ClaimType.EXPERIENCE,
                Sentiment.NEUTRAL,
                0.95,
                List.of(evidence));
        ChunkAnalysis chunk = new ChunkAnalysis(List.of(topic), List.of(claim));
        DatasetAnalysisResult result = new DatasetAnalysisResult(
                List.of(topic),
                List.of(claim),
                new DecisionReport(
                        "The discussion contains one reported salary.",
                        List.of("A participant reported 65k."),
                        List.of(),
                        List.of("The sample is very small."),
                        List.of("Collect more salary reports."),
                        List.of()));
        when(analysisModel.analyzeChunk(anyString(), any(), any())).thenReturn(chunk);
        when(analysisModel.synthesize(anyString(), any(), any())).thenReturn(result);

        processor.process(analysisId);

        given().when().get("/api/analyses/{analysisId}", analysisId)
                .then().statusCode(200)
                .body("status", equalTo("COMPLETED"))
                .body("topicCount", equalTo(1))
                .body("claimCount", equalTo(1))
                .body("evidenceCount", equalTo(1))
                .body("report.executiveSummary", equalTo("The discussion contains one reported salary."))
                .body("report.limitations", hasSize(greaterThanOrEqualTo(3)))
                .body("topics", hasSize(1))
                .body("topics[0].claims", hasSize(1))
                .body("topics[0].claims[0].evidence[0].sourceType", equalTo("POST"))
                .body("topics[0].claims[0].evidence[0].sourceId", equalTo((int) postId));

        given().when().get("/api/datasets/{datasetId}/analyses", datasetId)
                .then().statusCode(200)
                .body("$", hasSize(1))
                .body("[0].status", equalTo("COMPLETED"));
    }
}
