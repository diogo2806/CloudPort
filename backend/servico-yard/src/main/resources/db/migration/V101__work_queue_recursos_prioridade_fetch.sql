ALTER TABLE work_queue_patio
    ADD COLUMN IF NOT EXISTS plano_guindaste_id BIGINT,
    ADD COLUMN IF NOT EXISTS recurso_cais_id BIGINT,
    ADD COLUMN IF NOT EXISTS equipamento_patio_id BIGINT;

ALTER TABLE ordem_trabalho_patio
    ADD COLUMN IF NOT EXISTS prioridade_busca BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX IF NOT EXISTS idx_work_queue_equipamento_patio
    ON work_queue_patio (equipamento_patio_id, visita_navio_id, sequencia_inicial);

CREATE INDEX IF NOT EXISTS idx_ordem_patio_prioridade_busca
    ON ordem_trabalho_patio (work_queue_id, prioridade_busca DESC, prioridade_operacional, sequencia_navio);
