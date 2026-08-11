package com.example.reddit.analysis;

import com.example.reddit.config.AnalysisConfig;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AnalysisChunkerTest {

    @Test
    void createsBoundedChunksWithoutSplittingSourceIdentity() {
        AnalysisConfig config = mock(AnalysisConfig.class);
        when(config.maxInputChars()).thenReturn(1_000);
        AnalysisChunker chunker = new AnalysisChunker(config);
        AnalysisSourceItem post = source(EvidenceSourceType.POST, 11, "a".repeat(750));
        AnalysisSourceItem comment = source(EvidenceSourceType.COMMENT, 22, "b".repeat(750));
        AnalysisDatasetSource dataset = new AnalysisDatasetSource(
                new AnalysisDatasetContext(1, "java", "jobs", null, 1, 1),
                List.of(post, comment));

        List<AnalysisChunk> chunks = chunker.chunk(dataset);

        assertEquals(2, chunks.size());
        assertEquals(1, chunks.getFirst().number());
        assertEquals(2, chunks.getFirst().total());
        assertTrue(chunks.getFirst().content().contains("SOURCE POST:11"));
        assertTrue(chunks.get(1).content().contains("SOURCE COMMENT:22"));
        assertEquals(List.of(post), chunks.getFirst().sources());
        assertEquals(List.of(comment), chunks.get(1).sources());
    }

    private static AnalysisSourceItem source(EvidenceSourceType type, long id, String text) {
        return new AnalysisSourceItem(
                type,
                id,
                11,
                "reddit" + id,
                "https://reddit.test/post",
                "author",
                1,
                Instant.parse("2026-01-01T00:00:00Z"),
                text);
    }
}
