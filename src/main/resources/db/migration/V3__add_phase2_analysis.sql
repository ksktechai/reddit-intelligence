CREATE SEQUENCE analysis_run_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE analysis_topic_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE analysis_claim_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE analysis_evidence_SEQ START WITH 1 INCREMENT BY 50;
CREATE SEQUENCE analysis_report_SEQ START WITH 1 INCREMENT BY 50;

CREATE TABLE analysis_run (
    id BIGINT PRIMARY KEY,
    dataset_id BIGINT NOT NULL REFERENCES dataset(id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    model VARCHAR(200) NOT NULL,
    prompt_version VARCHAR(50) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    error_message TEXT,
    input_source_count INTEGER NOT NULL DEFAULT 0 CHECK (input_source_count >= 0),
    chunk_count INTEGER NOT NULL DEFAULT 0 CHECK (chunk_count >= 0),
    topic_count INTEGER NOT NULL DEFAULT 0 CHECK (topic_count >= 0),
    claim_count INTEGER NOT NULL DEFAULT 0 CHECK (claim_count >= 0),
    evidence_count INTEGER NOT NULL DEFAULT 0 CHECK (evidence_count >= 0),
    CONSTRAINT ck_analysis_run_status CHECK (
        status IN ('PENDING', 'RUNNING', 'COMPLETED', 'FAILED'))
);

CREATE TABLE analysis_topic (
    id BIGINT PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES analysis_run(id) ON DELETE CASCADE,
    name VARCHAR(200) NOT NULL,
    summary TEXT NOT NULL,
    sentiment VARCHAR(20) NOT NULL,
    sentiment_score DOUBLE PRECISION NOT NULL CHECK (
        sentiment_score >= -1.0 AND sentiment_score <= 1.0),
    mention_count INTEGER NOT NULL CHECK (mention_count >= 0),
    CONSTRAINT ck_analysis_topic_sentiment CHECK (
        sentiment IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL', 'MIXED')),
    CONSTRAINT uq_analysis_topic_run_name UNIQUE (run_id, name)
);

CREATE TABLE analysis_claim (
    id BIGINT PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES analysis_run(id) ON DELETE CASCADE,
    topic_id BIGINT NOT NULL REFERENCES analysis_topic(id) ON DELETE CASCADE,
    claim_text TEXT NOT NULL,
    claim_type VARCHAR(30) NOT NULL,
    sentiment VARCHAR(20) NOT NULL,
    confidence DOUBLE PRECISION NOT NULL CHECK (confidence >= 0.0 AND confidence <= 1.0),
    support_count INTEGER NOT NULL CHECK (support_count >= 0),
    contradict_count INTEGER NOT NULL CHECK (contradict_count >= 0),
    CONSTRAINT ck_analysis_claim_type CHECK (
        claim_type IN ('EXPERIENCE', 'OPINION', 'FACTUAL_ASSERTION', 'RECOMMENDATION')),
    CONSTRAINT ck_analysis_claim_sentiment CHECK (
        sentiment IN ('POSITIVE', 'NEGATIVE', 'NEUTRAL', 'MIXED'))
);

CREATE TABLE analysis_evidence (
    id BIGINT PRIMARY KEY,
    claim_id BIGINT NOT NULL REFERENCES analysis_claim(id) ON DELETE CASCADE,
    post_id BIGINT REFERENCES reddit_post(id) ON DELETE CASCADE,
    comment_id BIGINT REFERENCES reddit_comment(id) ON DELETE CASCADE,
    stance VARCHAR(20) NOT NULL,
    excerpt TEXT NOT NULL,
    rationale TEXT NOT NULL,
    CONSTRAINT ck_analysis_evidence_stance CHECK (
        stance IN ('SUPPORTS', 'CONTRADICTS', 'CONTEXT')),
    CONSTRAINT ck_analysis_evidence_one_source CHECK (
        (post_id IS NOT NULL AND comment_id IS NULL)
        OR (post_id IS NULL AND comment_id IS NOT NULL))
);

CREATE TABLE analysis_report (
    id BIGINT PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES analysis_run(id) ON DELETE CASCADE,
    executive_summary TEXT NOT NULL,
    key_findings_json TEXT NOT NULL,
    opportunities_json TEXT NOT NULL,
    risks_json TEXT NOT NULL,
    recommendations_json TEXT NOT NULL,
    limitations_json TEXT NOT NULL,
    CONSTRAINT uq_analysis_report_run UNIQUE (run_id)
);

CREATE INDEX idx_analysis_run_dataset_created ON analysis_run(dataset_id, created_at DESC);
CREATE INDEX idx_analysis_run_status ON analysis_run(status);
CREATE UNIQUE INDEX uq_analysis_run_active_dataset ON analysis_run(dataset_id)
    WHERE status IN ('PENDING', 'RUNNING');
CREATE INDEX idx_analysis_topic_run ON analysis_topic(run_id);
CREATE INDEX idx_analysis_claim_run ON analysis_claim(run_id);
CREATE INDEX idx_analysis_claim_topic ON analysis_claim(topic_id);
CREATE INDEX idx_analysis_evidence_claim ON analysis_evidence(claim_id);
CREATE INDEX idx_analysis_evidence_post ON analysis_evidence(post_id) WHERE post_id IS NOT NULL;
CREATE INDEX idx_analysis_evidence_comment ON analysis_evidence(comment_id) WHERE comment_id IS NOT NULL;
