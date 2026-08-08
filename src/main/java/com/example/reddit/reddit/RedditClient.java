package com.example.reddit.reddit;

import java.util.List;

public interface RedditClient {

    List<RedditPostData> searchPosts(
            String subreddit, String query, String sort, String timeRange, int maxPosts);

    RedditCommentThread fetchComments(String postRedditId);
}
