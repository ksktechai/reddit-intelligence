CREATE SEQUENCE dataset_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE reddit_post_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE reddit_comment_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE dataset (
    id BIGINT PRIMARY KEY,
    subreddit VARCHAR(100) NOT NULL,
    query VARCHAR(500) NOT NULL,
    sort VARCHAR(20) NOT NULL,
    time_range VARCHAR(20) NOT NULL,
    max_posts INTEGER NOT NULL CHECK (max_posts BETWEEN 1 AND 1000),
    include_comments BOOLEAN NOT NULL,
    status VARCHAR(30) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ,
    posts_imported INTEGER NOT NULL DEFAULT 0 CHECK (posts_imported >= 0),
    comments_imported INTEGER NOT NULL DEFAULT 0 CHECK (comments_imported >= 0),
    error_message TEXT,
    CONSTRAINT ck_dataset_status CHECK (status IN ('RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE reddit_post (
    id BIGINT PRIMARY KEY,
    reddit_id VARCHAR(32) NOT NULL,
    subreddit VARCHAR(100) NOT NULL,
    title TEXT NOT NULL,
    body TEXT,
    author VARCHAR(255),
    score INTEGER NOT NULL DEFAULT 0,
    permalink TEXT,
    external_url TEXT,
    created_at TIMESTAMPTZ,
    collected_at TIMESTAMPTZ NOT NULL,
    comment_count_reported INTEGER NOT NULL DEFAULT 0 CHECK (comment_count_reported >= 0),
    comments_downloaded BOOLEAN NOT NULL DEFAULT FALSE,
    comments_complete BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_reddit_post_reddit_id UNIQUE (reddit_id)
);

CREATE TABLE reddit_comment (
    id BIGINT PRIMARY KEY,
    reddit_id VARCHAR(32) NOT NULL,
    post_id BIGINT NOT NULL REFERENCES reddit_post(id) ON DELETE CASCADE,
    parent_comment_id BIGINT REFERENCES reddit_comment(id) ON DELETE CASCADE,
    author VARCHAR(255),
    body TEXT,
    score INTEGER NOT NULL DEFAULT 0,
    depth INTEGER NOT NULL CHECK (depth >= 0),
    created_at TIMESTAMPTZ,
    collected_at TIMESTAMPTZ NOT NULL,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT uq_reddit_comment_reddit_id UNIQUE (reddit_id)
);

CREATE TABLE dataset_post (
    dataset_id BIGINT NOT NULL REFERENCES dataset(id) ON DELETE CASCADE,
    post_id BIGINT NOT NULL REFERENCES reddit_post(id) ON DELETE CASCADE,
    PRIMARY KEY (dataset_id, post_id)
);

CREATE INDEX idx_dataset_created_at ON dataset(created_at DESC);
CREATE INDEX idx_dataset_post_post_id ON dataset_post(post_id);
CREATE INDEX idx_reddit_comment_post_id ON reddit_comment(post_id);
CREATE INDEX idx_reddit_comment_parent_id ON reddit_comment(parent_comment_id);
CREATE INDEX idx_reddit_comment_post_depth ON reddit_comment(post_id, depth);
