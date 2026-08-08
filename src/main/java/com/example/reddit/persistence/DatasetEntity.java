package com.example.reddit.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.LinkedHashSet;
import java.util.Set;

@Entity
@Table(name = "dataset")
public class DatasetEntity extends PanacheEntity {

    @Column(nullable = false, length = 100)
    public String subreddit;

    @Column(nullable = false, length = 500)
    public String query;

    @Column(nullable = false, length = 20)
    public String sort;

    @Column(name = "time_range", nullable = false, length = 20)
    public String timeRange;

    @Column(name = "max_posts", nullable = false)
    public int maxPosts;

    @Column(name = "include_comments", nullable = false)
    public boolean includeComments;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public DatasetStatus status;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "completed_at")
    public Instant completedAt;

    @Column(name = "posts_imported", nullable = false)
    public int postsImported;

    @Column(name = "comments_imported", nullable = false)
    public int commentsImported;

    @Column(name = "error_message", columnDefinition = "text")
    public String errorMessage;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "dataset_post",
            joinColumns = @JoinColumn(name = "dataset_id"),
            inverseJoinColumns = @JoinColumn(name = "post_id"))
    public Set<RedditPostEntity> posts = new LinkedHashSet<>();
}
