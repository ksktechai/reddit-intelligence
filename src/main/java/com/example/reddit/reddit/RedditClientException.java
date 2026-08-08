package com.example.reddit.reddit;

public class RedditClientException extends RuntimeException {
    public RedditClientException(String message) {
        super(message);
    }

    public RedditClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
