CREATE TABLE IF NOT EXISTS aplicacao_plano_otimizado_patio (
    id BIGSERIAL PRIMARY KEY,
    plano_id VARCHAR(100) NOT NULL,
    visita_navio_id BIGINT NOT NULL,
    status VARCHAR(30) NOT NULL,
    resultado_json TEXT,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_aplicacao_plano_otimizado_patio UNIQUE (plano_id, visita_navio_id)
);

CREATE INDEX IF NOT EXISTS idx_aplicacao_plano_otimizado_patio_status
    ON aplicacao_plano_otimizado_patio (status, atualizado_em);
