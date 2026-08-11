package com.example.reddit.analysis;

import java.util.List;

public record DecisionReport(
        String executiveSummary,
        List<String> keyFindings,
        List<String> opportunities,
        List<String> risks,
        List<String> recommendations,
        List<String> limitations) {
}
