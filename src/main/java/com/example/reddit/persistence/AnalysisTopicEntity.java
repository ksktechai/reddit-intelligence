package com.example.reddit.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "analysis_topic", uniqueConstraints = @UniqueConstraint(
        name = "uq_analysis_topic_run_name", columnNames = {"run_id", "name"}))
public class AnalysisTopicEntity extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false, updatable = false)
    public AnalysisRunEntity run;

    @Column(nullable = false, length = 200)
    public String name;

    @Column(nullable = false, columnDefinition = "text")
    public String summary;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Sentiment sentiment;

    @Column(name = "sentiment_score", nullable = false)
    public double sentimentScore;

    @Column(name = "mention_count", nullable = false)
    public int mentionCount;
}
