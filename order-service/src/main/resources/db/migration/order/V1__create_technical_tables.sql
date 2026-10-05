-- V1: technical tables for the transactional outbox and idempotent consumer.
-- ADR-0001 (outbox), ADR-0011 (consumer claim), REQ-FUNC-010, REQ-FUNC-016.

CREATE TABLE outbox_events (
                               event_id     uuid         PRIMARY KEY,
                               event_type   varchar(64)  NOT NULL,
                               aggregate_id varchar(64)  NOT NULL,
                               topic        varchar(128) NOT NULL,
                               payload      jsonb        NOT NULL,
                               occurred_at  timestamptz  NOT NULL,
                               published_at timestamptz  NULL
);

CREATE INDEX ix_outbox_pending
    ON outbox_events (occurred_at)
    WHERE published_at IS NULL;

CREATE TABLE processed_events (
                                  event_id     uuid         NOT NULL,
                                  consumer     varchar(64)  NOT NULL,
                                  processed_at timestamptz  NOT NULL,
                                  PRIMARY KEY (event_id, consumer)
);
