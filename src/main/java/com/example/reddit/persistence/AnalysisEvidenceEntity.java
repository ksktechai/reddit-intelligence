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
@Table(name = "analysis_evidence")
public class AnalysisEvidenceEntity extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "claim_id", nullable = false, updatable = false)
    public AnalysisClaimEntity claim;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", updatable = false)
    public RedditPostEntity post;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", updatable = false)
    public RedditCommentEntity comment;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    public EvidenceStance stance;

    @Column(nullable = false, columnDefinition = "text")
    public String excerpt;

    @Column(nullable = false, columnDefinition = "text")
    public String rationale;
}
