CREATE TABLE IF NOT EXISTS work_queue_patio (
    id BIGSERIAL PRIMARY KEY,
    identificador VARCHAR(120) NOT NULL,
    visita_navio_id BIGINT NOT NULL,
    berco VARCHAR(60),
    porao INTEGER,
    bloco_zona VARCHAR(60),
    sequencia_inicial INTEGER,
    pow VARCHAR(80),
    pool_operacional VARCHAR(80),
    equipamento VARCHAR(80),
    status VARCHAR(20) NOT NULL,
    prioridade_operacional INTEGER,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_work_queue_patio_identificador UNIQUE (identificador),
    CONSTRAINT ck_work_queue_patio_status CHECK (status IN ('ATIVA', 'INATIVA'))
);

CREATE INDEX IF NOT EXISTS idx_work_queue_patio_visita_status
    ON work_queue_patio (visita_navio_id, status, sequencia_inicial);

CREATE INDEX IF NOT EXISTS idx_work_queue_patio_prioridade
    ON work_queue_patio (status, prioridade_operacional, criado_em);
