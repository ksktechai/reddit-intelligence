package com.example.reddit.analysis;

import com.example.reddit.config.AnalysisConfig;
import jakarta.annotation.PreDestroy;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@ApplicationScoped
public class AnalysisTaskExecutor {
    private final ExecutorService executor;

    public AnalysisTaskExecutor(AnalysisConfig config) {
        int concurrency = Math.max(1, config.maxConcurrentAnalyses());
        executor = Executors.newFixedThreadPool(
                concurrency,
                Thread.ofVirtual().name("reddit-analysis-", 0).factory());
    }

    public void submit(Runnable task) {
        executor.submit(task);
    }

    @PreDestroy
    void close() {
        executor.close();
    }
}
