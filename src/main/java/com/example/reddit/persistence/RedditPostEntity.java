package com.example.reddit.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "reddit_post", uniqueConstraints = @UniqueConstraint(
        name = "uq_reddit_post_reddit_id", columnNames = "reddit_id"))
public class RedditPostEntity extends PanacheEntity {

    @Column(name = "reddit_id", nullable = false, length = 32, updatable = false)
    public String redditId;

    @Column(nullable = false, length = 100)
    public String subreddit;

    @Column(nullable = false, columnDefinition = "text")
    public String title;

    @Column(columnDefinition = "text")
    public String body;

    @Column(length = 255)
    public String author;

    @Column(nullable = false)
    public int score;

    @Column(columnDefinition = "text")
    public String permalink;

    @Column(name = "external_url", columnDefinition = "text")
    public String externalUrl;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "collected_at", nullable = false)
    public Instant collectedAt;

    @Column(name = "comment_count_reported", nullable = false)
    public int commentCountReported;

    @Column(name = "comments_downloaded", nullable = false)
    public boolean commentsDownloaded;

    @Column(name = "comments_complete", nullable = false)
    public boolean commentsComplete;
}
