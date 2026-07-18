CREATE TABLE IF NOT EXISTS aplicacao_plano_otimizado_navio_patio (
    id BIGSERIAL PRIMARY KEY,
    plano_id VARCHAR(100) NOT NULL,
    visita_navio_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    resultado_json TEXT,
    erro VARCHAR(2000),
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_aplicacao_plano_otimizado_navio_patio UNIQUE (plano_id, visita_navio_id)
);

CREATE INDEX IF NOT EXISTS idx_aplicacao_plano_navio_patio_status
    ON aplicacao_plano_otimizado_navio_patio (status, atualizado_em);
