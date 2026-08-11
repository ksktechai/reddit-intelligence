package com.example.reddit.analysis;

import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

@ApplicationScoped
public class AnalysisResultValidator {
    private static final Logger LOG = Logger.getLogger(AnalysisResultValidator.class);
    private static final int MAX_TOPICS = 12;
    private static final int MAX_CLAIMS = 20;
    private static final int MAX_EVIDENCE_PER_CLAIM = 2;
    private static final List<String> REQUIRED_LIMITATIONS = List.of(
            "Reddit participants are self-selected and are not representative of the wider population.",
            "Reddit claims and experiences were not independently verified.",
            "Search-provider coverage and incomplete comment threads may omit relevant discussion.");

    public DatasetAnalysisResult validate(
            AnalysisDatasetSource source,
            DatasetAnalysisResult candidate) {
        if (candidate == null || candidate.report() == null) {
            throw new AnalysisModelException("Ollama analysis did not include a decision report");
        }

        Map<String, AnalysisSourceItem> sources = new LinkedHashMap<>();
        source.sources().forEach(item -> sources.put(key(item.sourceType(), item.sourceId()), item));

        Map<String, ExtractedTopic> topicsByName = new LinkedHashMap<>();
        for (ExtractedTopic topic : safe(candidate.topics())) {
            if (topic == null || blank(topic.name()) || blank(topic.summary()) || topic.sentiment() == null) {
                continue;
            }
            String normalizedName = normalizeKey(topic.name());
            topicsByName.putIfAbsent(normalizedName, new ExtractedTopic(
                    limit(topic.name().trim(), 200),
                    limit(topic.summary().trim(), 600),
                    topic.sentiment(),
                    clamp(topic.sentimentScore(), -1.0, 1.0),
                    Math.max(0, topic.mentionCount())));
            if (topicsByName.size() == MAX_TOPICS) {
                break;
            }
        }

        List<ExtractedClaim> claims = new ArrayList<>();
        Set<String> seenClaims = new LinkedHashSet<>();
        for (ExtractedClaim claim : safe(candidate.claims())) {
            if (claim == null || blank(claim.topic()) || blank(claim.text())
                    || claim.type() == null || claim.sentiment() == null) {
                continue;
            }
            ExtractedTopic topic = topicsByName.get(normalizeKey(claim.topic()));
            if (topic == null) {
                LOG.warnf("Dropping analysis claim with unknown topic topic=\"%s\"", claim.topic());
                continue;
            }

            List<ExtractedEvidence> evidence = validEvidence(claim.evidence(), sources);
            if (evidence.isEmpty()) {
                LOG.warnf("Dropping analysis claim without valid evidence claim=\"%s\"", claim.text());
                continue;
            }
            String claimKey = normalizeKey(topic.name()) + "|" + normalizeKey(claim.text());
            if (!seenClaims.add(claimKey)) {
                continue;
            }
            claims.add(new ExtractedClaim(
                    topic.name(),
                    limit(claim.text().trim(), 600),
                    claim.type(),
                    claim.sentiment(),
                    clamp(claim.confidence(), 0.0, 1.0),
                    evidence));
            if (claims.size() == MAX_CLAIMS) {
                break;
            }
        }

        return new DatasetAnalysisResult(
                List.copyOf(topicsByName.values()),
                List.copyOf(claims),
                validatedReport(candidate.report()));
    }

    private static List<ExtractedEvidence> validEvidence(
            List<ExtractedEvidence> candidates,
            Map<String, AnalysisSourceItem> sources) {
        List<ExtractedEvidence> valid = new ArrayList<>();
        Set<String> seen = new LinkedHashSet<>();
        for (ExtractedEvidence evidence : safe(candidates)) {
            if (evidence == null || evidence.sourceType() == null || evidence.stance() == null
                    || evidence.sourceId() < 1 || blank(evidence.excerpt()) || blank(evidence.rationale())) {
                continue;
            }
            AnalysisSourceItem source = sources.get(key(evidence.sourceType(), evidence.sourceId()));
            if (source == null || !containsExcerpt(source.text(), evidence.excerpt())) {
                LOG.warnf("Dropping invalid analysis evidence source=%s:%d",
                        evidence.sourceType(), evidence.sourceId());
                continue;
            }
            String evidenceKey = key(evidence.sourceType(), evidence.sourceId())
                    + "|" + evidence.stance() + "|" + normalizeText(evidence.excerpt());
            if (!seen.add(evidenceKey)) {
                continue;
            }
            valid.add(new ExtractedEvidence(
                    evidence.sourceType(),
                    evidence.sourceId(),
                    evidence.stance(),
                    limit(evidence.excerpt().trim(), 240),
                    limit(evidence.rationale().trim(), 160)));
            if (valid.size() == MAX_EVIDENCE_PER_CLAIM) {
                break;
            }
        }
        return List.copyOf(valid);
    }

    private static DecisionReport validatedReport(DecisionReport report) {
        List<String> limitations = cleanList(report.limitations(), 8, 500);
        LinkedHashSet<String> combinedLimitations = new LinkedHashSet<>(limitations);
        combinedLimitations.addAll(REQUIRED_LIMITATIONS);
        return new DecisionReport(
                blank(report.executiveSummary())
                        ? "No reliable executive summary was generated."
                        : limit(report.executiveSummary().trim(), 1_500),
                cleanList(report.keyFindings(), 8, 500),
                cleanList(report.opportunities(), 8, 500),
                cleanList(report.risks(), 8, 500),
                cleanList(report.recommendations(), 8, 500),
                List.copyOf(combinedLimitations).subList(
                        0, Math.min(combinedLimitations.size(), 8)));
    }

    private static List<String> cleanList(List<String> values, int maxItems, int maxLength) {
        LinkedHashSet<String> cleaned = new LinkedHashSet<>();
        for (String value : safe(values)) {
            if (!blank(value)) {
                cleaned.add(limit(value.trim(), maxLength));
            }
            if (cleaned.size() == maxItems) {
                break;
            }
        }
        return List.copyOf(cleaned);
    }

    private static boolean containsExcerpt(String source, String excerpt) {
        return normalizeText(source).contains(normalizeText(excerpt));
    }

    private static String normalizeText(String value) {
        return value == null
                ? ""
                : value.replaceAll("\\s+", " ").trim().toLowerCase(Locale.ROOT);
    }

    private static String normalizeKey(String value) {
        return normalizeText(value).replaceAll("[^a-z0-9]+", " ").trim();
    }

    private static String key(EvidenceSourceType type, long id) {
        return type + ":" + id;
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }

    private static String limit(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static double clamp(double value, double minimum, double maximum) {
        if (!Double.isFinite(value)) {
            return 0.0;
        }
        return Math.max(minimum, Math.min(maximum, value));
    }

    private static <T> List<T> safe(List<T> values) {
        return values == null ? List.of() : values;
    }
}
