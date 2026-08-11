package com.example.reddit.analysis;

import com.example.reddit.config.AnalysisConfig;
import jakarta.enterprise.context.ApplicationScoped;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AnalysisChunker {
    private final AnalysisConfig config;

    public AnalysisChunker(AnalysisConfig config) {
        this.config = config;
    }

    public List<AnalysisChunk> chunk(AnalysisDatasetSource dataset) {
        int maxChars = Math.max(1_000, config.maxInputChars());
        List<DraftChunk> drafts = new ArrayList<>();
        StringBuilder content = new StringBuilder();
        List<AnalysisSourceItem> sources = new ArrayList<>();

        for (AnalysisSourceItem source : dataset.sources()) {
            String sourceBlock = formatSource(source, maxChars);
            if (!content.isEmpty() && content.length() + sourceBlock.length() > maxChars) {
                drafts.add(new DraftChunk(content.toString(), List.copyOf(sources)));
                content = new StringBuilder();
                sources = new ArrayList<>();
            }
            content.append(sourceBlock);
            sources.add(source);
        }
        if (!content.isEmpty()) {
            drafts.add(new DraftChunk(content.toString(), List.copyOf(sources)));
        }

        int total = drafts.size();
        List<AnalysisChunk> chunks = new ArrayList<>(total);
        for (int index = 0; index < total; index++) {
            DraftChunk draft = drafts.get(index);
            chunks.add(new AnalysisChunk(index + 1, total, draft.content(), draft.sources()));
        }
        return List.copyOf(chunks);
    }

    private static String formatSource(AnalysisSourceItem source, int maxChars) {
        String header = """
                SOURCE %s:%d
                POST_ID: %d
                REDDIT_ID: %s
                PERMALINK: %s
                AUTHOR: %s
                SCORE: %d
                CREATED_AT: %s
                TEXT:
                """.formatted(
                source.sourceType(),
                source.sourceId(),
                source.postId(),
                nullSafe(source.redditId()),
                nullSafe(source.permalink()),
                nullSafe(source.author()),
                source.score(),
                source.createdAt());
        String footer = "\nEND SOURCE " + source.reference() + "\n\n";
        int availableTextChars = Math.max(0, maxChars - header.length() - footer.length());
        String text = nullSafe(source.text());
        if (text.length() > availableTextChars) {
            text = text.substring(0, availableTextChars);
        }
        return header + text + footer;
    }

    private static String nullSafe(Object value) {
        return value == null ? "" : value.toString();
    }

    private record DraftChunk(String content, List<AnalysisSourceItem> sources) {
    }
}
