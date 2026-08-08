package com.example.reddit.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "reddit_comment", uniqueConstraints = @UniqueConstraint(
        name = "uq_reddit_comment_reddit_id", columnNames = "reddit_id"))
public class RedditCommentEntity extends PanacheEntity {

    @Column(name = "reddit_id", nullable = false, length = 32, updatable = false)
    public String redditId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "post_id", nullable = false, updatable = false)
    public RedditPostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "parent_comment_id")
    public RedditCommentEntity parentComment;

    @Column(length = 255)
    public String author;

    @Column(columnDefinition = "text")
    public String body;

    @Column(nullable = false)
    public int score;

    @Column(nullable = false)
    public int depth;

    @Column(name = "created_at")
    public Instant createdAt;

    @Column(name = "collected_at", nullable = false)
    public Instant collectedAt;

    @Column(nullable = false)
    public boolean deleted;
}
