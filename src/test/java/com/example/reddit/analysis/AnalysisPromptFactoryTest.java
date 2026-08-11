package com.example.reddit.analysis;

import com.example.reddit.persistence.ClaimType;
import com.example.reddit.persistence.EvidenceStance;
import com.example.reddit.persistence.Sentiment;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisPromptFactoryTest {

    @Test
    void compactsChunkResultsBeforeDatasetSynthesis() {
        List<ExtractedEvidence> evidence = IntStream.range(0, 3)
                .mapToObj(index -> new ExtractedEvidence(
                        EvidenceSourceType.POST,
                        1,
                        EvidenceStance.SUPPORTS,
                        "excerpt-" + index,
                        "rationale-" + index))
                .toList();
        List<ExtractedClaim> claims = IntStream.range(0, 25)
                .mapToObj(index -> new ExtractedClaim(
                        "topic",
                        "claim-" + index,
                        ClaimType.OPINION,
                        Sentiment.NEUTRAL,
                        0.8,
                        evidence))
                .toList();
        AnalysisPromptFactory factory = new AnalysisPromptFactory(new ObjectMapper());

        String prompt = factory.synthesisMessages(
                        new AnalysisDatasetContext(1, "java", "jobs", null, 1, 0),
                        List.of(new ChunkAnalysis(List.of(), claims)),
                        new ObjectMapper().createObjectNode())
                .get(1)
                .content();

        assertTrue(prompt.contains("\"text\":\"claim-19\""));
        assertFalse(prompt.contains("\"text\":\"claim-20\""));
        assertTrue(prompt.contains("\"excerpt\":\"excerpt-1\""));
        assertFalse(prompt.contains("\"excerpt\":\"excerpt-2\""));
    }
}
