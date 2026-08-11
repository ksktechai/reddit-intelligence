package com.example.reddit.api;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.quarkus.test.junit.QuarkusTest;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

@QuarkusTest
class OpenApiDocumentationTest {
    private static final List<String> HTTP_METHODS = List.of(
            "get", "post", "put", "patch", "delete", "head", "options");

    @Test
    void documentsEveryPublicOperationAndResponseBody() throws Exception {
        String document = given()
                .queryParam("format", "json")
                .when().get("/q/openapi")
                .then().statusCode(200)
                .extract().asString();
        JsonNode openApi = new ObjectMapper().readTree(document);

        assertFalse(openApi.path("info").path("description").asText().isBlank());
        Set<String> operationIds = new HashSet<>();
        openApi.path("paths").forEach(path -> HTTP_METHODS.forEach(method -> {
            JsonNode operation = path.path(method);
            if (operation.isObject()) {
                assertFalse(operation.path("summary").asText().isBlank(), method + " summary");
                assertFalse(operation.path("description").asText().isBlank(), method + " description");
                assertTrue(operationIds.add(operation.path("operationId").asText()), "unique operationId");
            }
        }));

        assertEquals(Set.of(
                "createDataset",
                "listDatasets",
                "getDataset",
                "listDatasetPosts",
                "getPost",
                "listPostComments",
                "getComment",
                "startDatasetAnalysis",
                "listDatasetAnalyses",
                "getAnalysis"), operationIds);
        assertEquals(
                "#/components/schemas/Dataset",
                openApi.at("/paths/~1api~1datasets/post/responses/201/content/application~1json/schema/$ref").asText());
        assertEquals(
                "#/components/schemas/AnalysisRunSummary",
                openApi.at("/paths/~1api~1datasets~1{datasetId}~1analyses/post/responses/202/content/application~1json/schema/$ref")
                        .asText());
    }
}
