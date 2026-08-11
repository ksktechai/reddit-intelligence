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

import java.time.Instant;

@Entity
@Table(name = "analysis_run")
public class AnalysisRunEntity extends PanacheEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "dataset_id", nullable = false, updatable = false)
    public DatasetEntity dataset;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    public AnalysisStatus status;

    @Column(nullable = false, length = 200)
    public String model;

    @Column(name = "prompt_version", nullable = false, length = 50)
    public String promptVersion;

    @Column(name = "created_at", nullable = false)
    public Instant createdAt;

    @Column(name = "started_at")
    public Instant startedAt;

    @Column(name = "completed_at")
    public Instant completedAt;

    @Column(name = "error_message", columnDefinition = "text")
    public String errorMessage;

    @Column(name = "input_source_count", nullable = false)
    public int inputSourceCount;

    @Column(name = "chunk_count", nullable = false)
    public int chunkCount;

    @Column(name = "topic_count", nullable = false)
    public int topicCount;

    @Column(name = "claim_count", nullable = false)
    public int claimCount;

    @Column(name = "evidence_count", nullable = false)
    public int evidenceCount;
}
