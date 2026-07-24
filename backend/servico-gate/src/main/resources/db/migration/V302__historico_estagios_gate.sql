CREATE TABLE gate_stage_event (
    id BIGSERIAL PRIMARY KEY,
    truck_visit_id BIGINT NOT NULL REFERENCES truck_visit (id) ON DELETE CASCADE,
    transaction_id BIGINT REFERENCES gate_transaction (id) ON DELETE CASCADE,
    stage_origem_id BIGINT REFERENCES gate_stage_config (id),
    stage_destino_id BIGINT NOT NULL REFERENCES gate_stage_config (id),
    lane_id BIGINT REFERENCES gate_lane (id),
    usuario VARCHAR(120) NOT NULL,
    correlation_id VARCHAR(120),
    tarefas_concluidas JSONB NOT NULL DEFAULT '[]'::JSONB,
    ocorrido_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gate_stage_event_visit
    ON gate_stage_event (truck_visit_id, ocorrido_em DESC);

CREATE INDEX idx_gate_stage_event_transaction
    ON gate_stage_event (transaction_id, ocorrido_em DESC);
