package com.example.reddit.analysis;

import com.example.reddit.persistence.ClaimType;
import com.example.reddit.persistence.EvidenceStance;
import com.example.reddit.persistence.Sentiment;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AnalysisResultValidatorTest {

    @Test
    void keepsOnlyEvidenceThatReferencesStoredVerbatimSourceText() {
        AnalysisSourceItem source = new AnalysisSourceItem(
                EvidenceSourceType.POST,
                11,
                11,
                "reddit11",
                "https://reddit.test/post",
                "author",
                3,
                Instant.parse("2026-01-01T00:00:00Z"),
                "Title: Graduate salary\nBody: The starting salary was 65k per year.");
        AnalysisDatasetSource dataset = new AnalysisDatasetSource(
                new AnalysisDatasetContext(1, "PersonalFinanceNZ", "graduate salary", null, 1, 0),
                List.of(source));
        ExtractedTopic topic = new ExtractedTopic(
                "Graduate pay", "Discussion of entry salaries", Sentiment.MIXED, 0.2, 1);
        ExtractedClaim validClaim = new ExtractedClaim(
                "graduate pay",
                "One participant reported a 65k starting salary.",
                ClaimType.EXPERIENCE,
                Sentiment.NEUTRAL,
                1.4,
                List.of(
                        new ExtractedEvidence(
                                EvidenceSourceType.POST,
                                11,
                                EvidenceStance.SUPPORTS,
                                "The starting salary was 65k per year.",
                                "Direct participant report"),
                        new ExtractedEvidence(
                                EvidenceSourceType.POST,
                                999,
                                EvidenceStance.SUPPORTS,
                                "Invented quotation",
                                "Invalid source")));
        ExtractedClaim unsupportedClaim = new ExtractedClaim(
                "Graduate pay",
                "An unsupported assertion.",
                ClaimType.FACTUAL_ASSERTION,
                Sentiment.NEUTRAL,
                0.5,
                List.of(new ExtractedEvidence(
                        EvidenceSourceType.POST,
                        11,
                        EvidenceStance.SUPPORTS,
                        "This text does not exist.",
                        "Hallucinated")));
        DatasetAnalysisResult candidate = new DatasetAnalysisResult(
                List.of(topic),
                List.of(validClaim, unsupportedClaim),
                new DecisionReport("Summary", List.of("Finding"), List.of(), List.of(), List.of(), List.of()));

        DatasetAnalysisResult validated = new AnalysisResultValidator().validate(dataset, candidate);

        assertEquals(1, validated.topics().size());
        assertEquals(1, validated.claims().size());
        assertEquals(1, validated.claims().getFirst().evidence().size());
        assertEquals(1.0, validated.claims().getFirst().confidence());
        assertTrue(validated.report().limitations().size() >= 3);
    }
}
