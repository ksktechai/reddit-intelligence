package com.example.reddit.reddit;

import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import jakarta.inject.Inject;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

@QuarkusTest
@QuarkusTestResource(value = CrawloraApiTestResource.class, restrictToAnnotatedClass = true)
class CrawloraApiIntegrationTest {

    @Inject
    @RestClient
    CrawloraApi api;

    @Test
    void returnsRateLimitResponseInsteadOfThrowingDefaultMapperException() {
        try (Response response = api.search(
                "quarkus", "java", "relevance", "all", 1, null, "test-key")) {
            assertEquals(429, response.getStatus());
            assertEquals("0", response.getHeaderString("Retry-After"));
        }
    }
}
