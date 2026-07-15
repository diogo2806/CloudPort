ALTER TABLE ordem_trabalho_patio
    ADD COLUMN IF NOT EXISTS work_queue_id BIGINT;

CREATE INDEX IF NOT EXISTS idx_ordem_patio_work_queue
    ON ordem_trabalho_patio (work_queue_id, prioridade_operacional, sequencia_navio);

CREATE TABLE IF NOT EXISTS historico_operacao_patio (
    id BIGSERIAL PRIMARY KEY,
    work_queue_id BIGINT,
    ordem_trabalho_patio_id BIGINT,
    acao VARCHAR(50) NOT NULL,
    usuario VARCHAR(120) NOT NULL,
    motivo VARCHAR(500),
    detalhes VARCHAR(2000),
    criado_em TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_historico_operacao_work_queue
    ON historico_operacao_patio (work_queue_id, criado_em DESC);

CREATE INDEX IF NOT EXISTS idx_historico_operacao_ordem
    ON historico_operacao_patio (ordem_trabalho_patio_id, criado_em DESC);
