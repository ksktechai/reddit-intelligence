package com.example.reddit.reddit;

import com.example.reddit.config.CrawloraConfig;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CrawloraHttpLoggingFilterTest {
    private static final String API_KEY = "secret-api-key";

    private CrawloraHttpLoggingFilter filter;

    @BeforeEach
    void setUp() {
        CrawloraConfig config = mock(CrawloraConfig.class);
        when(config.httpLogging()).thenReturn(true);
        when(config.apiKey()).thenReturn(Optional.of(API_KEY));
        filter = new CrawloraHttpLoggingFilter(config);
    }

    @Test
    void redactsSensitiveHeadersAndConfiguredApiKey() {
        MultivaluedHashMap<String, Object> headers = new MultivaluedHashMap<>();
        headers.add("x-api-key", API_KEY);
        headers.add("Authorization", "Bearer " + API_KEY);
        headers.add("X-Debug", "key=" + API_KEY);

        String rendered = filter.formatHeaders(headers);

        assertFalse(rendered.contains(API_KEY));
        assertTrue(rendered.contains("x-api-key=[REDACTED]"));
        assertTrue(rendered.contains("Authorization=[REDACTED]"));
        assertTrue(rendered.contains("X-Debug=key=[REDACTED]"));
    }

    @Test
    void responseLoggingPreservesTheRawEntityForTheResponseParser() throws Exception {
        String rawBody = "{\"code\":503,\"message\":\"temporary failure\"}";
        ClientRequestContext request = mock(ClientRequestContext.class);
        ClientResponseContext response = mock(ClientResponseContext.class);
        MultivaluedHashMap<String, String> responseHeaders = new MultivaluedHashMap<>();
        responseHeaders.add("Retry-After", "30");
        when(request.getMethod()).thenReturn("GET");
        when(request.getUri()).thenReturn(URI.create("https://api.crawlora.net/api/v1/reddit/search?q=jobs"));
        when(request.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        when(request.hasEntity()).thenReturn(false);
        when(response.getEntityStream()).thenReturn(
                new ByteArrayInputStream(rawBody.getBytes(StandardCharsets.UTF_8)));
        when(response.getHeaders()).thenReturn(responseHeaders);
        when(response.getStatus()).thenReturn(503);

        filter.filter(request);
        filter.filter(request, response);

        var entityStreamCaptor = org.mockito.ArgumentCaptor.forClass(InputStream.class);
        verify(response).setEntityStream(entityStreamCaptor.capture());
        assertEquals(rawBody,
                new String(entityStreamCaptor.getValue().readAllBytes(), StandardCharsets.UTF_8));
        verify(request, times(2)).setProperty(any(String.class), any());
    }
}
