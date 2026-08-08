package com.example.reddit.persistence;

import com.example.reddit.api.CreateDatasetRequest;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

import java.time.Instant;

@ApplicationScoped
public class DatasetPersistenceService {

    @Transactional
    public DatasetEntity create(CreateDatasetRequest request) {
        DatasetEntity dataset = new DatasetEntity();
        dataset.subreddit = request.subreddit();
        dataset.query = request.query();
        dataset.sort = request.sort();
        dataset.timeRange = request.timeRange();
        dataset.fromDate = request.fromDate();
        dataset.maxPosts = request.maxPosts();
        dataset.includeComments = request.includeComments();
        dataset.status = DatasetStatus.RUNNING;
        dataset.createdAt = Instant.now();
        dataset.persistAndFlush();
        return dataset;
    }

    @Transactional
    public void complete(long datasetId, int postsImported, int commentsImported) {
        DatasetEntity dataset = required(datasetId);
        dataset.status = DatasetStatus.COMPLETED;
        dataset.postsImported = postsImported;
        dataset.commentsImported = commentsImported;
        dataset.completedAt = Instant.now();
        dataset.errorMessage = null;
    }

    @Transactional
    public void fail(long datasetId, String errorMessage) {
        DatasetEntity dataset = required(datasetId);
        dataset.status = DatasetStatus.FAILED;
        dataset.completedAt = Instant.now();
        dataset.errorMessage = errorMessage;
    }

    private static DatasetEntity required(long id) {
        DatasetEntity dataset = DatasetEntity.findById(id);
        if (dataset == null) {
            throw new IllegalArgumentException("Dataset not found: " + id);
        }
        return dataset;
    }
}
