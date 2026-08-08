package com.example.reddit.config;

import io.smallrye.config.ConfigMapping;

import java.util.Optional;

@ConfigMapping(prefix = "crawlora")
public interface CrawloraConfig {
    Optional<String> apiKey();

    int commentsLimit();

    int maxRetries();

    long retryDelay();

    long rateLimitRetryDelay();
}
