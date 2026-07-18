-- DATA1180: reconciliacao auditavel entre BAPLIE, plano, inventario e execucao

CREATE TABLE IF NOT EXISTS reconciliacao_baplie_execucao (
    id BIGSERIAL PRIMARY KEY,
    estivagem_plan_id BIGINT NOT NULL REFERENCES estivagem_plan(id),
    bay_plan_id BIGINT NOT NULL REFERENCES bay_plan(id),
    visita_navio_id BIGINT,
    versao_plano BIGINT,
    status VARCHAR(30) NOT NULL,
    total_unidades INTEGER NOT NULL DEFAULT 0,
    total_divergencias INTEGER NOT NULL DEFAULT 0,
    total_criticas_abertas INTEGER NOT NULL DEFAULT 0,
    solicitante VARCHAR(120) NOT NULL,
    executada_em TIMESTAMP NOT NULL,
    concluida_em TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS divergencia_reconciliacao_baplie (
    id BIGSERIAL PRIMARY KEY,
    reconciliacao_id BIGINT NOT NULL REFERENCES reconciliacao_baplie_execucao(id) ON DELETE CASCADE,
    slot_navio_id BIGINT REFERENCES slot_navio(id) ON DELETE SET NULL,
    codigo_container VARCHAR(30) NOT NULL,
    tipo_divergencia VARCHAR(30) NOT NULL,
    severidade VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    campo VARCHAR(60) NOT NULL,
    fonte_referencia VARCHAR(30) NOT NULL,
    valor_referencia TEXT,
    fonte_divergente VARCHAR(30) NOT NULL,
    valor_divergente TEXT,
    decisao_resolucao VARCHAR(30),
    motivo_resolucao VARCHAR(1000),
    responsavel_resolucao VARCHAR(120),
    detectada_em TIMESTAMP NOT NULL,
    resolvida_em TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_reconciliacao_baplie_plano_execucao
    ON reconciliacao_baplie_execucao(estivagem_plan_id, executada_em DESC);

CREATE INDEX IF NOT EXISTS idx_divergencia_reconciliacao_aberta
    ON divergencia_reconciliacao_baplie(reconciliacao_id, status, severidade);

CREATE INDEX IF NOT EXISTS idx_divergencia_reconciliacao_container
    ON divergencia_reconciliacao_baplie(codigo_container);
