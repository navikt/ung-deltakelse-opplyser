CREATE TABLE IF NOT EXISTS outbox
(
    id               UUID PRIMARY KEY NOT NULL DEFAULT gen_random_uuid(),
    event_type       VARCHAR(255)     NOT NULL,
    aggregate_id     VARCHAR(255)     NOT NULL,
    payload          JSONB            NOT NULL,
    created_at       TIMESTAMP        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at     TIMESTAMP,
    status           VARCHAR(50)      NOT NULL DEFAULT 'PENDING',
    retry_count      INTEGER          NOT NULL DEFAULT 0,
    max_retry_count  INTEGER          NOT NULL DEFAULT 3,
    error_message    TEXT,
    next_retry_at    TIMESTAMP
);

-- Index for efficient queries
CREATE INDEX IF NOT EXISTS idx_outbox_status_created_at ON outbox (status, created_at);
CREATE INDEX IF NOT EXISTS idx_outbox_next_retry_at ON outbox (next_retry_at) WHERE next_retry_at IS NOT NULL;
