package com.example.reddit.analysis;

import com.example.reddit.persistence.Sentiment;

public record ExtractedTopic(
        String name,
        String summary,
        Sentiment sentiment,
        double sentimentScore,
        int mentionCount) {
}
