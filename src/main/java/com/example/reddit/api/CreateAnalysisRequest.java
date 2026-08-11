package com.example.reddit.api;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

@Schema(name = "CreateAnalysisRequest", description = "Optional model selection for a new analysis run.")
public record CreateAnalysisRequest(
        @Schema(
                description = "Ollama model name. Omit this field to use the server's configured LLM_MODEL.",
                example = "gpt-oss:20b")
        @Size(max = 200)
        @Pattern(regexp = "^[A-Za-z0-9._:/-]+$", message = "must be a valid Ollama model name")
        String model) {

    public CreateAnalysisRequest {
        model = model == null || model.isBlank() ? null : model.trim();
    }
}
