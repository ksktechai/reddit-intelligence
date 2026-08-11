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

@Entity
@Table(name = "analysis_claim")
public class AnalysisClaimEntity extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false, updatable = false)
    public AnalysisRunEntity run;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "topic_id", nullable = false, updatable = false)
    public AnalysisTopicEntity topic;

    @Column(name = "claim_text", nullable = false, columnDefinition = "text")
    public String claimText;

    @Enumerated(EnumType.STRING)
    @Column(name = "claim_type", nullable = false, length = 30)
    public ClaimType claimType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public Sentiment sentiment;

    @Column(nullable = false)
    public double confidence;

    @Column(name = "support_count", nullable = false)
    public int supportCount;

    @Column(name = "contradict_count", nullable = false)
    public int contradictCount;
}
