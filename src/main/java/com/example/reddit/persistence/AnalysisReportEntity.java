package com.example.reddit.persistence;

import io.quarkus.hibernate.orm.panache.PanacheEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "analysis_report", uniqueConstraints = @UniqueConstraint(
        name = "uq_analysis_report_run", columnNames = "run_id"))
public class AnalysisReportEntity extends PanacheEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "run_id", nullable = false, updatable = false)
    public AnalysisRunEntity run;

    @Column(name = "executive_summary", nullable = false, columnDefinition = "text")
    public String executiveSummary;

    @Column(name = "key_findings_json", nullable = false, columnDefinition = "text")
    public String keyFindingsJson;

    @Column(name = "opportunities_json", nullable = false, columnDefinition = "text")
    public String opportunitiesJson;

    @Column(name = "risks_json", nullable = false, columnDefinition = "text")
    public String risksJson;

    @Column(name = "recommendations_json", nullable = false, columnDefinition = "text")
    public String recommendationsJson;

    @Column(name = "limitations_json", nullable = false, columnDefinition = "text")
    public String limitationsJson;
}
