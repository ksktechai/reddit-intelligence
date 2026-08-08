package com.example.reddit.config;

import io.smallrye.config.ConfigMapping;

@ConfigMapping(prefix = "reddit")
public interface RedditConfig {
    String baseUrl();

    String userAgent();

    long connectTimeout();

    long readTimeout();

    int maxRetries();

    long retryDelay();
}
