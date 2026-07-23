CREATE TABLE caso_purgatorio_work_instruction (
    id BIGSERIAL PRIMARY KEY,
    ordem_trabalho_patio_id BIGINT NOT NULL,
    work_queue_id BIGINT NOT NULL,
    causa VARCHAR(30) NOT NULL,
    severidade VARCHAR(20) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    chave_idempotencia VARCHAR(120) NOT NULL,
    usuario VARCHAR(120) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    origem VARCHAR(120),
    correlation_id VARCHAR(120),
    snapshot_original TEXT NOT NULL,
    snapshot_atual TEXT,
    evidencias TEXT,
    historico TEXT NOT NULL,
    resolucao VARCHAR(500),
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    resolvido_em TIMESTAMP,
    CONSTRAINT uk_purgatorio_chave_idempotencia UNIQUE (chave_idempotencia),
    CONSTRAINT fk_purgatorio_ordem FOREIGN KEY (ordem_trabalho_patio_id) REFERENCES ordem_trabalho_patio(id)
);

CREATE INDEX idx_purgatorio_fila_estado
    ON caso_purgatorio_work_instruction (work_queue_id, estado);

CREATE INDEX idx_purgatorio_ordem_estado
    ON caso_purgatorio_work_instruction (ordem_trabalho_patio_id, estado);
