CREATE TABLE crane_sequence (
    id BIGSERIAL PRIMARY KEY,
    movement_id VARCHAR(120) NOT NULL,
    vessel_visit_id VARCHAR(120) NOT NULL,
    crane_id VARCHAR(80) NOT NULL,
    load_unit_id VARCHAR(120) NOT NULL,
    planned_start TIMESTAMP NOT NULL,
    started_at TIMESTAMP NULL,
    finished_at TIMESTAMP NULL,
    status VARCHAR(20) NOT NULL,
    operator_id VARCHAR(120) NULL,
    notes VARCHAR(4000) NULL,
    created_at TIMESTAMP NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_crane_sequence_movement UNIQUE (movement_id),
    CONSTRAINT ck_crane_sequence_status CHECK (status IN ('PLANNED', 'STARTED', 'FINISHED', 'PAUSED', 'CANCELLED'))
);

CREATE INDEX idx_crane_sequence_vessel_visit ON crane_sequence (vessel_visit_id);
CREATE INDEX idx_crane_sequence_status ON crane_sequence (status);
CREATE INDEX idx_crane_sequence_planned_start ON crane_sequence (planned_start);

CREATE TABLE crane_sequence_audit (
    id BIGSERIAL PRIMARY KEY,
    movement_id VARCHAR(120) NOT NULL,
    type VARCHAR(40) NOT NULL,
    status_before VARCHAR(20) NULL,
    status_after VARCHAR(20) NULL,
    operator_id VARCHAR(120) NOT NULL,
    reason VARCHAR(1000) NULL,
    occurred_at TIMESTAMP NOT NULL,
    CONSTRAINT ck_crane_sequence_audit_status_before CHECK (
        status_before IS NULL OR status_before IN ('PLANNED', 'STARTED', 'FINISHED', 'PAUSED', 'CANCELLED')),
    CONSTRAINT ck_crane_sequence_audit_status_after CHECK (
        status_after IS NULL OR status_after IN ('PLANNED', 'STARTED', 'FINISHED', 'PAUSED', 'CANCELLED'))
);

CREATE INDEX idx_crane_sequence_audit_movement
    ON crane_sequence_audit (movement_id, occurred_at);

CREATE TABLE crane_sequence_outbox (
    id BIGSERIAL PRIMARY KEY,
    event_key VARCHAR(180) NOT NULL,
    movement_id VARCHAR(120) NOT NULL,
    event_type VARCHAR(80) NOT NULL,
    payload VARCHAR(4000) NOT NULL,
    created_at TIMESTAMP NOT NULL,
    published_at TIMESTAMP NULL,
    CONSTRAINT uk_crane_sequence_outbox_event UNIQUE (event_key)
);

CREATE INDEX idx_crane_sequence_outbox_pending
    ON crane_sequence_outbox (published_at, created_at);
