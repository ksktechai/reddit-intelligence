package com.example.reddit.reddit;

import java.util.List;

public record RedditSearchPage(List<RedditPostData> posts, String after) {
    public RedditSearchPage {
        posts = posts == null ? List.of() : List.copyOf(posts);
    }
}
