package com.example.reddit.analysis;

public class AnalysisModelException extends RuntimeException {
    public AnalysisModelException(String message) {
        super(message);
    }

    public AnalysisModelException(String message, Throwable cause) {
        super(message, cause);
    }
}
