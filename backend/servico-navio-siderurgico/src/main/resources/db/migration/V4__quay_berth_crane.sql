CREATE TABLE IF NOT EXISTS plano_guindaste_visita (
    id BIGSERIAL PRIMARY KEY,
    visita_navio_id BIGINT NOT NULL REFERENCES visita_navio (id) ON DELETE CASCADE,
    codigo_guindaste VARCHAR(40) NOT NULL,
    recurso_cais VARCHAR(80),
    porao INTEGER NOT NULL,
    work_queue_id BIGINT,
    sequencia INTEGER NOT NULL,
    movimentos_planejados INTEGER NOT NULL,
    produtividade_planejada_mph NUMERIC(10, 2) NOT NULL,
    inicio_planejado TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    fim_planejado TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status VARCHAR(30) NOT NULL,
    berco VARCHAR(40) NOT NULL,
    usuario VARCHAR(120) NOT NULL,
    observacao VARCHAR(1000),
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_plano_guindaste_visita_sequencia UNIQUE (visita_navio_id, sequencia),
    CONSTRAINT ck_plano_guindaste_porao CHECK (porao > 0),
    CONSTRAINT ck_plano_guindaste_movimentos CHECK (movimentos_planejados > 0),
    CONSTRAINT ck_plano_guindaste_produtividade CHECK (produtividade_planejada_mph > 0),
    CONSTRAINT ck_plano_guindaste_janela CHECK (fim_planejado > inicio_planejado),
    CONSTRAINT ck_plano_guindaste_status CHECK (status IN ('RASCUNHO', 'PUBLICADO', 'EM_EXECUCAO', 'CONCLUIDO'))
);

CREATE INDEX IF NOT EXISTS idx_plano_guindaste_visita ON plano_guindaste_visita (visita_navio_id, sequencia);
CREATE INDEX IF NOT EXISTS idx_plano_guindaste_codigo ON plano_guindaste_visita (codigo_guindaste, inicio_planejado, fim_planejado);
CREATE INDEX IF NOT EXISTS idx_plano_guindaste_work_queue ON plano_guindaste_visita (work_queue_id);
