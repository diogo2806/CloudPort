-- BUS1360: paralisações, trocas de turno e handover por guindaste

CREATE TABLE IF NOT EXISTS evento_operacional_guindaste (
    id BIGSERIAL PRIMARY KEY,
    execucao_id BIGINT NOT NULL,
    guindaste_id INTEGER NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    natureza VARCHAR(30),
    inicio TIMESTAMP NOT NULL,
    fim TIMESTAMP,
    motivo VARCHAR(1000),
    impacto VARCHAR(1000),
    turno_origem VARCHAR(120),
    turno_destino VARCHAR(120),
    responsavel VARCHAR(120) NOT NULL,
    responsavel_destino VARCHAR(120),
    pendencias VARCHAR(2000),
    observacao VARCHAR(1000),
    encerrado_por VARCHAR(120),
    observacao_encerramento VARCHAR(1000),
    versao BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_evento_operacional_guindaste_execucao
        FOREIGN KEY (execucao_id) REFERENCES execucao_sequencia_guindaste (id) ON DELETE CASCADE,
    CONSTRAINT ck_evento_operacional_guindaste_id
        CHECK (guindaste_id > 0),
    CONSTRAINT ck_evento_operacional_guindaste_tipo
        CHECK (tipo IN ('PARALISACAO', 'HANDOVER')),
    CONSTRAINT ck_evento_operacional_guindaste_natureza
        CHECK (natureza IS NULL OR natureza IN ('PLANEJADA', 'OPERACIONAL')),
    CONSTRAINT ck_evento_operacional_guindaste_periodo
        CHECK (fim IS NULL OR fim >= inicio),
    CONSTRAINT ck_evento_operacional_guindaste_coerencia
        CHECK (
            (tipo = 'PARALISACAO' AND natureza IS NOT NULL AND motivo IS NOT NULL AND impacto IS NOT NULL)
            OR
            (tipo = 'HANDOVER' AND natureza IS NULL AND fim = inicio
                AND turno_origem IS NOT NULL AND turno_destino IS NOT NULL
                AND responsavel_destino IS NOT NULL AND pendencias IS NOT NULL)
        )
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_evento_guindaste_paralisacao_aberta
    ON evento_operacional_guindaste (execucao_id, guindaste_id)
    WHERE tipo = 'PARALISACAO' AND fim IS NULL;

CREATE INDEX IF NOT EXISTS idx_evento_guindaste_linha_tempo
    ON evento_operacional_guindaste (execucao_id, inicio DESC);

CREATE INDEX IF NOT EXISTS idx_evento_guindaste_intervalo
    ON evento_operacional_guindaste (execucao_id, guindaste_id, inicio, fim);
