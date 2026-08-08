package com.example.reddit.dataset;

import com.example.reddit.api.CreateDatasetRequest;
import com.example.reddit.api.DatasetResponse;
import com.example.reddit.persistence.DatasetEntity;
import com.example.reddit.persistence.DatasetPersistenceService;
import com.example.reddit.post.PostService;
import com.example.reddit.reddit.RedditImportService;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import jakarta.ws.rs.NotFoundException;
import org.jboss.logging.Logger;

import java.util.List;

@ApplicationScoped
public class DatasetService {
    private static final Logger LOG = Logger.getLogger(DatasetService.class);

    private final DatasetPersistenceService persistence;
    private final RedditImportService importer;
    private final PostService postService;

    @Inject
    public DatasetService(
            DatasetPersistenceService persistence,
            RedditImportService importer,
            PostService postService) {
        this.persistence = persistence;
        this.importer = importer;
        this.postService = postService;
    }

    public DatasetResponse create(CreateDatasetRequest request) {
        DatasetEntity dataset = persistence.create(request);
        try {
            ImportStatistics statistics = importer.importDataset(dataset.id, request);
            persistence.complete(dataset.id, statistics.postsImported(), statistics.commentsImported());
        } catch (RuntimeException exception) {
            LOG.errorf(exception, "Reddit import failed datasetId=%d subreddit=%s query=\"%s\"",
                    dataset.id, request.subreddit(), request.query());
            persistence.fail(dataset.id, safeMessage(exception));
        }
        return get(dataset.id);
    }

    @Transactional
    public List<DatasetResponse> list() {
        return DatasetEntity.<DatasetEntity>list("order by createdAt desc")
                .stream()
                .map(DatasetService::toResponse)
                .toList();
    }

    @Transactional
    public DatasetResponse get(long id) {
        DatasetEntity dataset = DatasetEntity.findById(id);
        if (dataset == null) {
            throw new NotFoundException("Dataset not found: " + id);
        }
        return toResponse(dataset);
    }

    public List<com.example.reddit.api.PostResponse> posts(long datasetId) {
        // Confirm the dataset exists so an unknown id is a 404 rather than an empty list.
        get(datasetId);
        return postService.forDataset(datasetId);
    }

    private static DatasetResponse toResponse(DatasetEntity dataset) {
        return new DatasetResponse(
                dataset.id,
                dataset.subreddit,
                dataset.query,
                dataset.sort,
                dataset.timeRange,
                dataset.fromDate,
                dataset.maxPosts,
                dataset.includeComments,
                dataset.postsImported,
                dataset.commentsImported,
                dataset.status,
                dataset.createdAt,
                dataset.completedAt,
                dataset.errorMessage);
    }

    private static String safeMessage(RuntimeException exception) {
        String message = exception.getMessage();
        return message == null || message.isBlank()
                ? exception.getClass().getSimpleName()
                : message;
    }
}
