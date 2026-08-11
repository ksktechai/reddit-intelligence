package com.example.reddit.analysis;

import com.example.reddit.api.AnalysisClaimResponse;
import com.example.reddit.api.AnalysisEvidenceResponse;
import com.example.reddit.api.AnalysisResponse;
import com.example.reddit.api.AnalysisRunSummaryResponse;
import com.example.reddit.api.AnalysisTopicResponse;
import com.example.reddit.api.CreateAnalysisRequest;
import com.example.reddit.api.DecisionReportResponse;
import com.example.reddit.config.AnalysisConfig;
import com.example.reddit.persistence.AnalysisClaimEntity;
import com.example.reddit.persistence.AnalysisEvidenceEntity;
import com.example.reddit.persistence.AnalysisPersistenceService;
import com.example.reddit.persistence.AnalysisReportEntity;
import com.example.reddit.persistence.AnalysisRunEntity;
import com.example.reddit.persistence.AnalysisTopicEntity;
import com.example.reddit.persistence.DatasetEntity;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;

import java.util.List;

@ApplicationScoped
public class AnalysisService {
    private static final TypeReference<List<String>> STRING_LIST = new TypeReference<>() {
    };

    private final AnalysisPersistenceService persistence;
    private final AnalysisTaskExecutor executor;
    private final AnalysisProcessor processor;
    private final AnalysisConfig config;
    private final ObjectMapper objectMapper;

    public AnalysisService(
            AnalysisPersistenceService persistence,
            AnalysisTaskExecutor executor,
            AnalysisProcessor processor,
            AnalysisConfig config,
            ObjectMapper objectMapper) {
        this.persistence = persistence;
        this.executor = executor;
        this.processor = processor;
        this.config = config;
        this.objectMapper = objectMapper;
    }

    public AnalysisRunSummaryResponse start(long datasetId, CreateAnalysisRequest request) {
        String model = request == null || request.model() == null
                ? config.model()
                : request.model();
        AnalysisRunEntity run = persistence.createRun(datasetId, model);
        try {
            executor.submit(() -> processor.process(run.id));
        } catch (RuntimeException exception) {
            persistence.fail(run.id, "Could not schedule analysis: " + exception.getMessage());
            throw exception;
        }
        return getSummary(run.id);
    }

    @Transactional
    public List<AnalysisRunSummaryResponse> listForDataset(long datasetId) {
        if (DatasetEntity.findById(datasetId) == null) {
            throw new NotFoundException("Dataset not found: " + datasetId);
        }
        return AnalysisRunEntity.<AnalysisRunEntity>find(
                        "dataset.id = ?1 order by createdAt desc", datasetId)
                .list()
                .stream()
                .map(AnalysisService::toSummary)
                .toList();
    }

    @Transactional
    public AnalysisRunSummaryResponse getSummary(long analysisId) {
        return toSummary(required(analysisId));
    }

    @Transactional
    public AnalysisResponse get(long analysisId) {
        AnalysisRunEntity run = required(analysisId);
        List<AnalysisTopicResponse> topics = AnalysisTopicEntity
                .<AnalysisTopicEntity>find(
                        "run.id = ?1 order by mentionCount desc, name asc", analysisId)
                .list()
                .stream()
                .map(this::toTopic)
                .toList();
        AnalysisReportEntity report = AnalysisReportEntity
                .find("run.id", analysisId)
                .firstResult();
        return new AnalysisResponse(
                run.id,
                run.dataset.id,
                run.status,
                run.model,
                run.promptVersion,
                run.createdAt,
                run.startedAt,
                run.completedAt,
                run.errorMessage,
                run.inputSourceCount,
                run.chunkCount,
                run.topicCount,
                run.claimCount,
                run.evidenceCount,
                report == null ? null : toReport(report),
                topics);
    }

    private AnalysisTopicResponse toTopic(AnalysisTopicEntity topic) {
        List<AnalysisClaimResponse> claims = AnalysisClaimEntity
                .<AnalysisClaimEntity>find(
                        "topic.id = ?1 order by confidence desc, id asc", topic.id)
                .list()
                .stream()
                .map(this::toClaim)
                .toList();
        return new AnalysisTopicResponse(
                topic.id,
                topic.name,
                topic.summary,
                topic.sentiment,
                topic.sentimentScore,
                topic.mentionCount,
                claims);
    }

    private AnalysisClaimResponse toClaim(AnalysisClaimEntity claim) {
        List<AnalysisEvidenceResponse> evidence = AnalysisEvidenceEntity
                .<AnalysisEvidenceEntity>find(
                        "claim.id = ?1 order by id asc", claim.id)
                .list()
                .stream()
                .map(AnalysisService::toEvidence)
                .toList();
        return new AnalysisClaimResponse(
                claim.id,
                claim.claimText,
                claim.claimType,
                claim.sentiment,
                claim.confidence,
                claim.supportCount,
                claim.contradictCount,
                evidence);
    }

    private static AnalysisEvidenceResponse toEvidence(AnalysisEvidenceEntity evidence) {
        boolean postSource = evidence.post != null;
        return new AnalysisEvidenceResponse(
                evidence.id,
                postSource ? EvidenceSourceType.POST : EvidenceSourceType.COMMENT,
                postSource ? evidence.post.id : evidence.comment.id,
                postSource ? evidence.post.redditId : evidence.comment.redditId,
                postSource ? evidence.post.permalink : evidence.comment.post.permalink,
                evidence.stance,
                evidence.excerpt,
                evidence.rationale);
    }

    private DecisionReportResponse toReport(AnalysisReportEntity report) {
        return new DecisionReportResponse(
                report.executiveSummary,
                strings(report.keyFindingsJson),
                strings(report.opportunitiesJson),
                strings(report.risksJson),
                strings(report.recommendationsJson),
                strings(report.limitationsJson));
    }

    private List<String> strings(String json) {
        try {
            return objectMapper.readValue(json, STRING_LIST);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Stored analysis report JSON is invalid", exception);
        }
    }

    private static AnalysisRunSummaryResponse toSummary(AnalysisRunEntity run) {
        return new AnalysisRunSummaryResponse(
                run.id,
                run.dataset.id,
                run.status,
                run.model,
                run.promptVersion,
                run.createdAt,
                run.startedAt,
                run.completedAt,
                run.errorMessage,
                run.inputSourceCount,
                run.chunkCount,
                run.topicCount,
                run.claimCount,
                run.evidenceCount);
    }

    private static AnalysisRunEntity required(long id) {
        AnalysisRunEntity run = AnalysisRunEntity.findById(id);
        if (run == null) {
            throw new NotFoundException("Analysis not found: " + id);
        }
        return run;
    }
}
