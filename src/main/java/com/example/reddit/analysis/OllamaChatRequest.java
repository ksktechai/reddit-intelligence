package com.example.reddit.analysis;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

import java.util.List;

public record OllamaChatRequest(
        String model,
        List<OllamaMessage> messages,
        boolean stream,
        double temperature,
        Integer seed,
        @JsonProperty("max_tokens") int maxTokens,
        @JsonProperty("reasoning_effort") String reasoningEffort,
        @JsonProperty("response_format") JsonNode responseFormat) {
}
