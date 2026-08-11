package com.example.reddit.analysis;

import com.example.reddit.persistence.AnalysisPersistenceService;
import jakarta.enterprise.context.ApplicationScoped;
import org.jboss.logging.Logger;

import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class AnalysisProcessor {
    private static final Logger LOG = Logger.getLogger(AnalysisProcessor.class);

    private final AnalysisPersistenceService persistence;
    private final AnalysisChunker chunker;
    private final AnalysisModel model;
    private final AnalysisResultValidator validator;

    public AnalysisProcessor(
            AnalysisPersistenceService persistence,
            AnalysisChunker chunker,
            AnalysisModel model,
            AnalysisResultValidator validator) {
        this.persistence = persistence;
        this.chunker = chunker;
        this.model = model;
        this.validator = validator;
    }

    public void process(long runId) {
        try {
            AnalysisRunContext run = persistence.begin(runId);
            AnalysisDatasetSource source = persistence.loadDatasetSource(run.datasetId());
            List<AnalysisChunk> chunks = chunker.chunk(source);
            if (chunks.isEmpty()) {
                throw new AnalysisModelException("Dataset has no usable Reddit text to analyse");
            }

            LOG.infof(
                    "Starting Phase 2 analysis runId=%d datasetId=%d model=%s sources=%d chunks=%d",
                    run.runId(),
                    run.datasetId(),
                    run.model(),
                    source.sources().size(),
                    chunks.size());
            List<ChunkAnalysis> chunkAnalyses = new ArrayList<>(chunks.size());
            for (AnalysisChunk chunk : chunks) {
                chunkAnalyses.add(model.analyzeChunk(run.model(), source.context(), chunk));
            }
            DatasetAnalysisResult candidate = model.synthesize(
                    run.model(), source.context(), List.copyOf(chunkAnalyses));
            DatasetAnalysisResult validated = validator.validate(source, candidate);
            persistence.complete(
                    run.runId(), validated, source.sources().size(), chunks.size());
            LOG.infof(
                    "Completed Phase 2 analysis runId=%d topics=%d claims=%d",
                    run.runId(), validated.topics().size(), validated.claims().size());
        } catch (RuntimeException exception) {
            LOG.errorf(exception, "Phase 2 analysis failed runId=%d", runId);
            try {
                persistence.fail(runId, safeMessage(exception));
            } catch (RuntimeException persistenceFailure) {
                LOG.errorf(persistenceFailure, "Could not mark Phase 2 analysis failed runId=%d", runId);
            }
        }
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message.substring(0, Math.min(message.length(), 4_000));
    }
}
