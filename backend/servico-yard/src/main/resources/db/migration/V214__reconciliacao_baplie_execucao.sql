-- V201: reconciliação persistida entre BAPLIE, plano, inventário e execução física

ALTER TABLE bay_plan_container
    ADD COLUMN IF NOT EXISTS bay_execucao INTEGER,
    ADD COLUMN IF NOT EXISTS row_execucao INTEGER,
    ADD COLUMN IF NOT EXISTS tier_execucao INTEGER,
    ADD COLUMN IF NOT EXISTS peso_execucao_kg DOUBLE PRECISION;

ALTER TABLE slot_navio
    ADD COLUMN IF NOT EXISTS status_reconciliacao VARCHAR(30) NOT NULL DEFAULT 'NAO_RECONCILIADO',
    ADD COLUMN IF NOT EXISTS severidade_reconciliacao VARCHAR(20),
    ADD COLUMN IF NOT EXISTS reconciliado_em TIMESTAMP;

CREATE TABLE IF NOT EXISTS divergencia_reconciliacao_slot (
    id BIGSERIAL PRIMARY KEY,
    estivagem_plan_id BIGINT NOT NULL,
    slot_navio_id BIGINT,
    chave VARCHAR(160) NOT NULL,
    codigo_container VARCHAR(40) NOT NULL,
    tipo VARCHAR(50) NOT NULL,
    severidade VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    valor_baplie TEXT,
    valor_plano TEXT,
    valor_inventario TEXT,
    valor_execucao TEXT,
    assinatura_fontes VARCHAR(64) NOT NULL,
    decisao VARCHAR(50),
    justificativa VARCHAR(1000),
    resolvido_por VARCHAR(150),
    resolvido_em TIMESTAMP,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    CONSTRAINT fk_divergencia_reconciliacao_plano
        FOREIGN KEY (estivagem_plan_id)
        REFERENCES estivagem_plan (id)
        ON DELETE CASCADE,
    CONSTRAINT fk_divergencia_reconciliacao_slot
        FOREIGN KEY (slot_navio_id)
        REFERENCES slot_navio (id)
        ON DELETE SET NULL,
    CONSTRAINT uk_divergencia_reconciliacao_chave
        UNIQUE (estivagem_plan_id, chave)
);

CREATE INDEX IF NOT EXISTS idx_divergencia_reconciliacao_plano_status
    ON divergencia_reconciliacao_slot (estivagem_plan_id, status, severidade);

CREATE INDEX IF NOT EXISTS idx_divergencia_reconciliacao_container
    ON divergencia_reconciliacao_slot (codigo_container);
