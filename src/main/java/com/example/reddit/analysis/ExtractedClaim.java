package com.example.reddit.analysis;

import com.example.reddit.persistence.ClaimType;
import com.example.reddit.persistence.Sentiment;

import java.util.List;

public record ExtractedClaim(
        String topic,
        String text,
        ClaimType type,
        Sentiment sentiment,
        double confidence,
        List<ExtractedEvidence> evidence) {
}
