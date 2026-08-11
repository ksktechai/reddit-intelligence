package com.example.reddit.analysis;

import com.example.reddit.config.AnalysisConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ProcessingException;
import jakarta.ws.rs.core.Response;
import org.eclipse.microprofile.rest.client.inject.RestClient;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class OllamaAnalysisModel implements AnalysisModel {
    private static final Logger LOG = Logger.getLogger(OllamaAnalysisModel.class);
    private static final long MAX_RETRY_DELAY_MS = 30_000L;

    private final OllamaOpenAiApi api;
    private final ObjectMapper objectMapper;
    private final AnalysisConfig config;
    private final AnalysisSchemas schemas;
    private final AnalysisPromptFactory prompts;

    @Inject
    public OllamaAnalysisModel(
            @RestClient OllamaOpenAiApi api,
            ObjectMapper objectMapper,
            AnalysisConfig config,
            AnalysisSchemas schemas,
            AnalysisPromptFactory prompts) {
        this.api = api;
        this.objectMapper = objectMapper;
        this.config = config;
        this.schemas = schemas;
        this.prompts = prompts;
    }

    @Override
    public ChunkAnalysis analyzeChunk(
            String model,
            AnalysisDatasetContext context,
            AnalysisChunk chunk) {
        JsonNode schema = schemas.chunkSchema();
        return invoke(
                "chunk " + chunk.number() + "/" + chunk.total(),
                model,
                prompts.chunkMessages(context, chunk, schema),
                schema,
                "reddit_chunk_analysis",
                "low",
                ChunkAnalysis.class);
    }

    @Override
    public DatasetAnalysisResult synthesize(
            String model,
            AnalysisDatasetContext context,
            List<ChunkAnalysis> chunkAnalyses) {
        JsonNode schema = schemas.resultSchema();
        return invoke(
                "dataset synthesis",
                model,
                prompts.synthesisMessages(context, chunkAnalyses, schema),
                schema,
                "reddit_dataset_analysis",
                "medium",
                DatasetAnalysisResult.class);
    }

    private <T> T invoke(
            String operation,
            String model,
            List<OllamaMessage> messages,
            JsonNode schema,
            String schemaName,
            String reasoningEffort,
            Class<T> resultType) {
        int maxRetries = Math.max(0, config.maxRetries());
        AnalysisModelException lastFailure = null;
        boolean compactRetry = false;

        for (int attempt = 0; attempt <= maxRetries; attempt++) {
            OllamaChatRequest request = new OllamaChatRequest(
                    model,
                    messagesForAttempt(messages, operation, compactRetry),
                    false,
                    config.temperature(),
                    42,
                    config.maxOutputTokens(),
                    reasoningEffort,
                    responseFormat(schemaName, schema));
            long started = System.nanoTime();
            LOG.infof("Ollama request operation=\"%s\" model=%s attempt=%d", operation, model, attempt + 1);
            if (config.logPayloads()) {
                LOG.infof("Ollama request payload operation=\"%s\" payload=%s",
                        operation, safeJson(request));
            }

            try (Response response = api.chat(request, "Bearer " + config.apiKey())) {
                int status = response.getStatus();
                String rawBody = response.readEntity(String.class);
                long durationMs = (System.nanoTime() - started) / 1_000_000L;
                LOG.infof("Ollama response operation=\"%s\" status=%d durationMs=%d",
                        operation, status, durationMs);
                if (config.logPayloads()) {
                    LOG.infof("Ollama response payload operation=\"%s\" payload=%s", operation, rawBody);
                }

                if (status >= 200 && status < 300) {
                    try {
                        return parseCompletion(rawBody, operation, resultType);
                    } catch (JsonProcessingException | IllegalArgumentException exception) {
                        boolean truncated = exception instanceof TruncatedCompletionException
                                || exception.getMessage() != null
                                && exception.getMessage().contains("Unexpected end-of-input");
                        compactRetry = compactRetry || truncated;
                        lastFailure = new AnalysisModelException(
                                truncated
                                        ? "Ollama truncated structured output for " + operation
                                                + " at max_tokens=" + config.maxOutputTokens()
                                        : "Ollama returned invalid structured output for " + operation,
                                exception);
                        LOG.warnf(
                                "Invalid Ollama structured output operation=\"%s\" attempt=%d truncated=%s error=\"%s\"",
                                operation,
                                attempt + 1,
                                truncated,
                                compact(exception.getMessage()));
                    }
                } else {
                    boolean retryable = status == 429 || status >= 500;
                    lastFailure = new AnalysisModelException(
                            "Ollama request failed for " + operation + " with HTTP " + status
                                    + providerError(rawBody));
                    if (!retryable) {
                        throw lastFailure;
                    }
                    LOG.warnf("Retryable Ollama HTTP failure operation=\"%s\" status=%d attempt=%d",
                            operation, status, attempt + 1);
                }
            } catch (ProcessingException exception) {
                lastFailure = new AnalysisModelException(
                        "Ollama transport failure for " + operation, exception);
                LOG.warnf(exception, "Ollama transport failure operation=\"%s\" attempt=%d",
                        operation, attempt + 1);
            }

            if (attempt < maxRetries) {
                sleep(backoff(attempt), operation);
            }
        }

        throw lastFailure == null
                ? new AnalysisModelException("Ollama request failed for " + operation)
                : lastFailure;
    }

    private <T> T parseCompletion(
            String rawBody,
            String operation,
            Class<T> resultType) throws JsonProcessingException {
        JsonNode envelope = objectMapper.readTree(rawBody);
        JsonNode choice = envelope.path("choices").path(0);
        JsonNode content = choice.path("message").path("content");
        String finishReason = choice.path("finish_reason").asText("unknown");
        int completionTokens = envelope.path("usage").path("completion_tokens").asInt(-1);
        int contentChars = content.isTextual() ? content.asText().length() : 0;
        LOG.infof(
                "Ollama completion operation=\"%s\" finishReason=%s completionTokens=%d contentChars=%d",
                operation,
                finishReason,
                completionTokens,
                contentChars);
        if ("length".equalsIgnoreCase(finishReason)) {
            throw new TruncatedCompletionException(
                    "finish_reason=length completion_tokens=" + completionTokens + " content_chars=" + contentChars);
        }
        if (!content.isTextual() || content.asText().isBlank()) {
            throw new IllegalArgumentException("Ollama response has no choices[0].message.content");
        }
        return objectMapper.readValue(content.asText(), resultType);
    }

    private static List<OllamaMessage> messagesForAttempt(
            List<OllamaMessage> original,
            String operation,
            boolean compactRetry) {
        if (!compactRetry) {
            return original;
        }
        List<OllamaMessage> messages = new ArrayList<>(original);
        String limits = "dataset synthesis".equals(operation)
                ? "Keep only the 12 strongest topics and 20 strongest claims, use at most 2 evidence "
                        + "items per claim and 8 items per report list. Keep excerpts under 240 characters, "
                        + "rationales under 160 characters, and the executive summary under 1,500 characters."
                : "Keep only the 10 strongest topics and 20 strongest claims, with at most 2 evidence "
                        + "items per claim. Keep excerpts under 240 characters and rationales under 160 characters.";
        messages.add(new OllamaMessage(
                "user",
                "The previous response reached the completion limit and was invalid JSON. Return a complete, "
                        + "substantially smaller replacement JSON document. " + limits));
        return List.copyOf(messages);
    }

    private ObjectNode responseFormat(String name, JsonNode schema) {
        ObjectNode responseFormat = objectMapper.createObjectNode();
        responseFormat.put("type", "json_schema");
        ObjectNode jsonSchema = responseFormat.putObject("json_schema");
        jsonSchema.put("name", name);
        jsonSchema.put("strict", true);
        jsonSchema.set("schema", schema);
        return responseFormat;
    }

    private long backoff(int attempt) {
        long base = Math.max(0L, config.retryDelay());
        int shift = Math.min(attempt, 16);
        if (base > (MAX_RETRY_DELAY_MS >> shift)) {
            return MAX_RETRY_DELAY_MS;
        }
        return Math.min(base << shift, MAX_RETRY_DELAY_MS);
    }

    private static void sleep(long milliseconds, String operation) {
        try {
            Thread.sleep(milliseconds);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new AnalysisModelException("Interrupted while retrying Ollama " + operation, exception);
        }
    }

    private String safeJson(Object value) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException exception) {
            return "<unserializable>";
        }
    }

    private static String providerError(String body) {
        if (body == null || body.isBlank()) {
            return "";
        }
        String compact = body.replaceAll("\\s+", " ").trim();
        return ": " + compact.substring(0, Math.min(compact.length(), 500));
    }

    private static String compact(String value) {
        if (value == null || value.isBlank()) {
            return "unknown";
        }
        String compact = value.replaceAll("\\s+", " ").trim();
        return compact.substring(0, Math.min(compact.length(), 500));
    }

    private static final class TruncatedCompletionException extends IllegalArgumentException {
        private TruncatedCompletionException(String message) {
            super(message);
        }
    }
}
