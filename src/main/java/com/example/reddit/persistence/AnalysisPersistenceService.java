package com.example.reddit.persistence;

import com.example.reddit.analysis.AnalysisDatasetContext;
import com.example.reddit.analysis.AnalysisDatasetSource;
import com.example.reddit.analysis.AnalysisRunContext;
import com.example.reddit.analysis.AnalysisSourceItem;
import com.example.reddit.analysis.DatasetAnalysisResult;
import com.example.reddit.analysis.EvidenceSourceType;
import com.example.reddit.analysis.ExtractedClaim;
import com.example.reddit.analysis.ExtractedEvidence;
import com.example.reddit.analysis.ExtractedTopic;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ClientErrorException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.core.Response;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@ApplicationScoped
public class AnalysisPersistenceService {
    public static final String PROMPT_VERSION = "phase2-v1";

    private final ObjectMapper objectMapper;

    public AnalysisPersistenceService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Transactional
    public AnalysisRunEntity createRun(long datasetId, String model) {
        DatasetEntity dataset = DatasetEntity.findById(datasetId);
        if (dataset == null) {
            throw new NotFoundException("Dataset not found: " + datasetId);
        }
        if (dataset.status != DatasetStatus.COMPLETED) {
            throw new ClientErrorException(
                    "Dataset must be COMPLETED before analysis: " + datasetId,
                    Response.Status.CONFLICT);
        }
        if (dataset.posts.isEmpty()) {
            throw new BadRequestException("Dataset has no posts to analyse: " + datasetId);
        }
        long activeRuns = AnalysisRunEntity.count(
                "dataset.id = ?1 and (status = ?2 or status = ?3)",
                datasetId,
                AnalysisStatus.PENDING,
                AnalysisStatus.RUNNING);
        if (activeRuns > 0) {
            throw new ClientErrorException(
                    "Dataset already has an active analysis: " + datasetId,
                    Response.Status.CONFLICT);
        }

        AnalysisRunEntity run = new AnalysisRunEntity();
        run.dataset = dataset;
        run.status = AnalysisStatus.PENDING;
        run.model = model;
        run.promptVersion = PROMPT_VERSION;
        run.createdAt = Instant.now();
        run.persistAndFlush();
        return run;
    }

    @Transactional
    public AnalysisRunContext begin(long runId) {
        AnalysisRunEntity run = requiredRun(runId);
        if (run.status != AnalysisStatus.PENDING) {
            throw new IllegalStateException("Analysis is not pending: " + runId);
        }
        run.status = AnalysisStatus.RUNNING;
        run.startedAt = Instant.now();
        run.errorMessage = null;
        return new AnalysisRunContext(run.id, run.dataset.id, run.model);
    }

    @Transactional
    public AnalysisDatasetSource loadDatasetSource(long datasetId) {
        DatasetEntity dataset = DatasetEntity.findById(datasetId);
        if (dataset == null) {
            throw new NotFoundException("Dataset not found: " + datasetId);
        }

        List<RedditPostEntity> posts = dataset.posts.stream()
                .sorted(Comparator.comparing(
                        (RedditPostEntity post) -> post.createdAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                        .thenComparing(post -> post.id))
                .toList();
        List<AnalysisSourceItem> sources = new ArrayList<>();
        int commentCount = 0;
        for (RedditPostEntity post : posts) {
            sources.add(new AnalysisSourceItem(
                    EvidenceSourceType.POST,
                    post.id,
                    post.id,
                    post.redditId,
                    post.permalink,
                    post.author,
                    post.score,
                    post.createdAt,
                    postText(post)));

            List<RedditCommentEntity> comments = RedditCommentEntity
                    .<RedditCommentEntity>find(
                            "post.id = ?1 order by createdAt asc, depth asc, id asc", post.id)
                    .list();
            for (RedditCommentEntity comment : comments) {
                if (comment.deleted || comment.body == null || comment.body.isBlank()) {
                    continue;
                }
                sources.add(new AnalysisSourceItem(
                        EvidenceSourceType.COMMENT,
                        comment.id,
                        post.id,
                        comment.redditId,
                        post.permalink,
                        comment.author,
                        comment.score,
                        comment.createdAt,
                        comment.body));
                commentCount++;
            }
        }

        return new AnalysisDatasetSource(
                new AnalysisDatasetContext(
                        dataset.id,
                        dataset.subreddit,
                        dataset.query,
                        dataset.fromDate,
                        posts.size(),
                        commentCount),
                List.copyOf(sources));
    }

    @Transactional
    public void complete(
            long runId,
            DatasetAnalysisResult result,
            int inputSourceCount,
            int chunkCount) {
        AnalysisRunEntity run = requiredRun(runId);
        if (run.status != AnalysisStatus.RUNNING) {
            throw new IllegalStateException("Analysis is not running: " + runId);
        }

        Map<String, AnalysisTopicEntity> topicsByName = new LinkedHashMap<>();
        for (ExtractedTopic topic : result.topics()) {
            AnalysisTopicEntity entity = new AnalysisTopicEntity();
            entity.run = run;
            entity.name = topic.name();
            entity.summary = topic.summary();
            entity.sentiment = topic.sentiment();
            entity.sentimentScore = topic.sentimentScore();
            entity.mentionCount = topic.mentionCount();
            entity.persistAndFlush();
            topicsByName.put(topic.name(), entity);
        }

        int evidenceCount = 0;
        for (ExtractedClaim claim : result.claims()) {
            AnalysisTopicEntity topic = topicsByName.get(claim.topic());
            if (topic == null) {
                continue;
            }
            AnalysisClaimEntity claimEntity = new AnalysisClaimEntity();
            claimEntity.run = run;
            claimEntity.topic = topic;
            claimEntity.claimText = claim.text();
            claimEntity.claimType = claim.type();
            claimEntity.sentiment = claim.sentiment();
            claimEntity.confidence = claim.confidence();
            claimEntity.supportCount = (int) claim.evidence().stream()
                    .filter(item -> item.stance() == EvidenceStance.SUPPORTS)
                    .count();
            claimEntity.contradictCount = (int) claim.evidence().stream()
                    .filter(item -> item.stance() == EvidenceStance.CONTRADICTS)
                    .count();
            claimEntity.persistAndFlush();

            for (ExtractedEvidence evidence : claim.evidence()) {
                AnalysisEvidenceEntity evidenceEntity = new AnalysisEvidenceEntity();
                evidenceEntity.claim = claimEntity;
                if (evidence.sourceType() == EvidenceSourceType.POST) {
                    evidenceEntity.post = RedditPostEntity.findById(evidence.sourceId());
                } else {
                    evidenceEntity.comment = RedditCommentEntity.findById(evidence.sourceId());
                }
                evidenceEntity.stance = evidence.stance();
                evidenceEntity.excerpt = evidence.excerpt();
                evidenceEntity.rationale = evidence.rationale();
                evidenceEntity.persist();
                evidenceCount++;
            }
        }

        AnalysisReportEntity report = new AnalysisReportEntity();
        report.run = run;
        report.executiveSummary = result.report().executiveSummary();
        report.keyFindingsJson = json(result.report().keyFindings());
        report.opportunitiesJson = json(result.report().opportunities());
        report.risksJson = json(result.report().risks());
        report.recommendationsJson = json(result.report().recommendations());
        report.limitationsJson = json(result.report().limitations());
        report.persist();

        run.status = AnalysisStatus.COMPLETED;
        run.completedAt = Instant.now();
        run.errorMessage = null;
        run.inputSourceCount = inputSourceCount;
        run.chunkCount = chunkCount;
        run.topicCount = result.topics().size();
        run.claimCount = result.claims().size();
        run.evidenceCount = evidenceCount;
    }

    @Transactional
    public void fail(long runId, String errorMessage) {
        AnalysisRunEntity run = requiredRun(runId);
        run.status = AnalysisStatus.FAILED;
        run.completedAt = Instant.now();
        run.errorMessage = errorMessage;
    }

    @Transactional
    public int failInterruptedRuns() {
        List<AnalysisRunEntity> interrupted = AnalysisRunEntity
                .<AnalysisRunEntity>find(
                        "status = ?1 or status = ?2",
                        AnalysisStatus.PENDING,
                        AnalysisStatus.RUNNING)
                .list();
        Instant now = Instant.now();
        for (AnalysisRunEntity run : interrupted) {
            run.status = AnalysisStatus.FAILED;
            run.completedAt = now;
            run.errorMessage = "Application restarted before the analysis completed";
        }
        return interrupted.size();
    }

    private String json(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not persist analysis report", exception);
        }
    }

    private static String postText(RedditPostEntity post) {
        String body = post.body == null ? "" : post.body;
        return "Title: " + post.title + "\nBody: " + body;
    }

    private static AnalysisRunEntity requiredRun(long id) {
        AnalysisRunEntity run = AnalysisRunEntity.findById(id);
        if (run == null) {
            throw new NotFoundException("Analysis not found: " + id);
        }
        return run;
    }
}
