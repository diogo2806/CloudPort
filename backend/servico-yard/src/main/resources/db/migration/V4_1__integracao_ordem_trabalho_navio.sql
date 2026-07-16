ALTER TABLE ordem_trabalho_patio ADD COLUMN IF NOT EXISTS visita_navio_id BIGINT;
ALTER TABLE ordem_trabalho_patio ADD COLUMN IF NOT EXISTS item_operacao_navio_id BIGINT;
ALTER TABLE ordem_trabalho_patio ADD COLUMN IF NOT EXISTS plano_estiva_navio_id BIGINT;
ALTER TABLE ordem_trabalho_patio ADD COLUMN IF NOT EXISTS tipo_origem VARCHAR(30);
ALTER TABLE ordem_trabalho_patio ADD COLUMN IF NOT EXISTS tipo_destino VARCHAR(30);
ALTER TABLE ordem_trabalho_patio ADD COLUMN IF NOT EXISTS sequencia_navio INTEGER;
ALTER TABLE ordem_trabalho_patio ADD COLUMN IF NOT EXISTS prioridade_operacional INTEGER;

CREATE INDEX IF NOT EXISTS idx_ordem_patio_visita_navio
    ON ordem_trabalho_patio (visita_navio_id, item_operacao_navio_id);

CREATE INDEX IF NOT EXISTS idx_ordem_patio_sequencia_navio
    ON ordem_trabalho_patio (visita_navio_id, sequencia_navio, prioridade_operacional);
