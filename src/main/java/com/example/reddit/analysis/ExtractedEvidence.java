package com.example.reddit.analysis;

import com.example.reddit.persistence.EvidenceStance;

public record ExtractedEvidence(
        EvidenceSourceType sourceType,
        long sourceId,
        EvidenceStance stance,
        String excerpt,
        String rationale) {
}
