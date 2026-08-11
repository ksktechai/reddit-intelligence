package com.example.reddit.analysis;

import com.example.reddit.config.AnalysisConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OllamaAnalysisModelTest {
    private final ObjectMapper objectMapper = new ObjectMapper();
    private OllamaOpenAiApi api;
    private AnalysisConfig config;
    private OllamaAnalysisModel model;

    @BeforeEach
    void setUp() {
        api = mock(OllamaOpenAiApi.class);
        config = mock(AnalysisConfig.class);
        when(config.apiKey()).thenReturn("test-key");
        when(config.maxRetries()).thenReturn(1);
        when(config.retryDelay()).thenReturn(0L);
        when(config.maxOutputTokens()).thenReturn(2_000);
        when(config.temperature()).thenReturn(0.0);
        AnalysisSchemas schemas = new AnalysisSchemas(objectMapper);
        model = new OllamaAnalysisModel(
                api,
                objectMapper,
                config,
                schemas,
                new AnalysisPromptFactory(objectMapper));
    }

    @Test
    void requestsJsonSchemaAndParsesStructuredContent() throws Exception {
        String content = """
                {"topics":[],"claims":[]}
                """;
        Response successful = response(200, completion(content));
        when(api.chat(any(), eq("Bearer test-key")))
                .thenReturn(successful);
        AnalysisChunk chunk = new AnalysisChunk(1, 1, "SOURCE POST:1", List.of());

        ChunkAnalysis result = model.analyzeChunk(
                "gpt-oss:20b",
                new AnalysisDatasetContext(1, "java", "jobs", null, 1, 0),
                chunk);

        assertEquals(List.of(), result.topics());
        ArgumentCaptor<OllamaChatRequest> request = ArgumentCaptor.forClass(OllamaChatRequest.class);
        verify(api).chat(request.capture(), eq("Bearer test-key"));
        assertEquals("gpt-oss:20b", request.getValue().model());
        assertEquals("json_schema", request.getValue().responseFormat().path("type").asText());
        assertTrue(request.getValue().responseFormat().path("json_schema").path("strict").asBoolean());
        JsonNode schema = request.getValue().responseFormat().path("json_schema").path("schema");
        assertFalse(schema.path("additionalProperties").asBoolean());
        assertTrue(schema.findValues("maxLength").isEmpty());
        assertTrue(schema.findValues("maxItems").isEmpty());
        assertTrue(schema.findValues("minimum").isEmpty());
        assertTrue(schema.findValues("maximum").isEmpty());
        assertEquals("low", request.getValue().reasoningEffort());
    }

    @Test
    void retriesMalformedStructuredContent() throws Exception {
        Response malformed = response(200, completion("not-json"));
        Response valid = response(200, completion("{\"topics\":[],\"claims\":[]}"));
        when(api.chat(any(), eq("Bearer test-key"))).thenReturn(malformed, valid);

        model.analyzeChunk(
                "gpt-oss:20b",
                new AnalysisDatasetContext(1, "java", "jobs", null, 1, 0),
                new AnalysisChunk(1, 1, "SOURCE POST:1", List.of()));

        verify(api, times(2)).chat(any(), eq("Bearer test-key"));
    }

    @Test
    void retriesTruncatedCompletionWithCompactOutputInstructions() throws Exception {
        Response truncated = response(200, completion("{\"topics\":[", "length", 2_000));
        Response valid = response(200, completion("{\"topics\":[],\"claims\":[]}"));
        when(api.chat(any(), eq("Bearer test-key"))).thenReturn(truncated, valid);

        model.analyzeChunk(
                "gpt-oss:20b",
                new AnalysisDatasetContext(1, "java", "jobs", null, 1, 0),
                new AnalysisChunk(1, 1, "SOURCE POST:1", List.of()));

        ArgumentCaptor<OllamaChatRequest> requests = ArgumentCaptor.forClass(OllamaChatRequest.class);
        verify(api, times(2)).chat(requests.capture(), eq("Bearer test-key"));
        List<OllamaMessage> retryMessages = requests.getAllValues().get(1).messages();
        assertTrue(retryMessages.getLast().content().contains("previous response reached the completion limit"));
        assertTrue(retryMessages.getLast().content().contains("20 strongest claims"));
    }

    private String completion(String content) throws Exception {
        return completion(content, "stop", 10);
    }

    private String completion(String content, String finishReason, int completionTokens) throws Exception {
        var choice = objectMapper.createObjectNode();
        choice.set("message", objectMapper.createObjectNode().put("content", content));
        choice.put("finish_reason", finishReason);
        var envelope = objectMapper.createObjectNode();
        envelope.set("choices", objectMapper.createArrayNode().add(choice));
        envelope.set("usage", objectMapper.createObjectNode().put("completion_tokens", completionTokens));
        return objectMapper.writeValueAsString(envelope);
    }

    private static Response response(int status, String body) {
        Response response = mock(Response.class);
        when(response.getStatus()).thenReturn(status);
        when(response.readEntity(String.class)).thenReturn(body);
        return response;
    }
}
