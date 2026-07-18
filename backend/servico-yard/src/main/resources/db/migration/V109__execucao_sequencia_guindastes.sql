-- BUS1150: execução planejada versus realizada da sequência de guindastes

CREATE TABLE IF NOT EXISTS execucao_sequencia_guindaste (
    id BIGSERIAL PRIMARY KEY,
    estivagem_plan_id BIGINT NOT NULL,
    status VARCHAR(40) NOT NULL,
    numero_guindastes INTEGER NOT NULL,
    janela_base_inicio TIMESTAMP NOT NULL,
    duracao_movimento_minutos INTEGER NOT NULL,
    reconciliado_em TIMESTAMP,
    reconciliado_por VARCHAR(120),
    observacao_reconciliacao VARCHAR(1000),
    versao BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_execucao_guindaste_plano UNIQUE (estivagem_plan_id),
    CONSTRAINT fk_execucao_guindaste_plano
        FOREIGN KEY (estivagem_plan_id) REFERENCES estivagem_plan (id),
    CONSTRAINT ck_execucao_guindaste_numero CHECK (numero_guindastes > 0),
    CONSTRAINT ck_execucao_guindaste_duracao CHECK (duracao_movimento_minutos > 0)
);

CREATE TABLE IF NOT EXISTS movimento_execucao_guindaste (
    id BIGSERIAL PRIMARY KEY,
    execucao_id BIGINT NOT NULL,
    ordem_planejada INTEGER NOT NULL,
    guindaste_id INTEGER NOT NULL,
    codigo_container VARCHAR(20) NOT NULL,
    bay INTEGER NOT NULL,
    row_bay INTEGER NOT NULL,
    tier INTEGER NOT NULL,
    tipo_operacao VARCHAR(30) NOT NULL,
    janela_inicio_planejada TIMESTAMP NOT NULL,
    janela_fim_planejada TIMESTAMP NOT NULL,
    quantidade_planejada NUMERIC(19, 3) NOT NULL DEFAULT 1,
    quantidade_realizada NUMERIC(19, 3) NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL,
    iniciado_em TIMESTAMP,
    iniciado_por VARCHAR(120),
    concluido_em TIMESTAMP,
    concluido_por VARCHAR(120),
    excecao VARCHAR(1000),
    motivo_replanejamento VARCHAR(1000),
    replanejado_em TIMESTAMP,
    replanejado_por VARCHAR(120),
    versao BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimento_execucao_guindaste
        FOREIGN KEY (execucao_id) REFERENCES execucao_sequencia_guindaste (id) ON DELETE CASCADE,
    CONSTRAINT uk_movimento_execucao_ordem UNIQUE (execucao_id, ordem_planejada),
    CONSTRAINT ck_movimento_execucao_ordem CHECK (ordem_planejada > 0),
    CONSTRAINT ck_movimento_execucao_guindaste CHECK (guindaste_id > 0),
    CONSTRAINT ck_movimento_execucao_janela CHECK (janela_inicio_planejada < janela_fim_planejada),
    CONSTRAINT ck_movimento_execucao_qtd_planejada CHECK (quantidade_planejada > 0),
    CONSTRAINT ck_movimento_execucao_qtd_realizada CHECK (quantidade_realizada >= 0)
);

CREATE INDEX IF NOT EXISTS idx_movimento_execucao_guindaste_status
    ON movimento_execucao_guindaste (execucao_id, status);

CREATE INDEX IF NOT EXISTS idx_movimento_execucao_guindaste_janela
    ON movimento_execucao_guindaste (guindaste_id, janela_inicio_planejada, janela_fim_planejada);
