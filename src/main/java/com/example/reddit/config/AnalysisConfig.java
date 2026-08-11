package com.example.reddit.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "llm")
public interface AnalysisConfig {
    String model();

    String apiKey();

    int maxRetries();

    long retryDelay();

    int maxInputChars();

    int maxOutputTokens();

    double temperature();

    boolean logPayloads();

    int maxConcurrentAnalyses();
}
