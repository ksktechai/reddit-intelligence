package com.example.reddit.reddit;

import java.util.List;

public record CrawloraSearchPage(List<RedditPostData> posts, String after) {
    public CrawloraSearchPage {
        posts = posts == null ? List.of() : List.copyOf(posts);
    }
}
