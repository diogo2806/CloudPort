CREATE TABLE IF NOT EXISTS custodia_exchange_area (
    id BIGSERIAL PRIMARY KEY,
    codigo_unidade VARCHAR(40) NOT NULL,
    area VARCHAR(80) NOT NULL,
    posicao VARCHAR(80) NOT NULL,
    equipamento_entrega VARCHAR(80) NOT NULL,
    operador_entrega VARCHAR(120) NOT NULL,
    condicao_entrega VARCHAR(120) NOT NULL,
    lacres_entrega VARCHAR(500) NOT NULL,
    entregue_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    chave_idempotencia_entrega VARCHAR(120) NOT NULL,
    equipamento_recebimento VARCHAR(80),
    operador_recebimento VARCHAR(120),
    condicao_recebimento VARCHAR(120),
    lacres_recebimento VARCHAR(500),
    recebido_em TIMESTAMP WITHOUT TIME ZONE,
    chave_idempotencia_recebimento VARCHAR(120),
    status VARCHAR(20) NOT NULL,
    bloqueada BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_divergencia VARCHAR(1000),
    versao BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_custodia_exchange_entrega UNIQUE (chave_idempotencia_entrega),
    CONSTRAINT uk_custodia_exchange_recebimento UNIQUE (chave_idempotencia_recebimento),
    CONSTRAINT ck_custodia_exchange_status CHECK (status IN ('ENTREGUE', 'RECEBIDA', 'DIVERGENTE')),
    CONSTRAINT ck_custodia_exchange_recebimento CHECK (
        (status = 'ENTREGUE' AND recebido_em IS NULL AND chave_idempotencia_recebimento IS NULL)
        OR
        (status IN ('RECEBIDA', 'DIVERGENTE')
            AND recebido_em IS NOT NULL
            AND chave_idempotencia_recebimento IS NOT NULL
            AND equipamento_recebimento IS NOT NULL
            AND operador_recebimento IS NOT NULL
            AND condicao_recebimento IS NOT NULL
            AND lacres_recebimento IS NOT NULL)
    ),
    CONSTRAINT ck_custodia_exchange_bloqueio CHECK (
        (status = 'DIVERGENTE' AND bloqueada = TRUE AND motivo_divergencia IS NOT NULL)
        OR
        (status <> 'DIVERGENTE' AND bloqueada = FALSE AND motivo_divergencia IS NULL)
    )
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_custodia_exchange_unidade_ativa
    ON custodia_exchange_area (UPPER(codigo_unidade))
    WHERE status IN ('ENTREGUE', 'DIVERGENTE');

CREATE INDEX IF NOT EXISTS idx_custodia_exchange_status_atualizacao
    ON custodia_exchange_area (status, atualizado_em DESC);

CREATE INDEX IF NOT EXISTS idx_custodia_exchange_area_posicao
    ON custodia_exchange_area (area, posicao, atualizado_em DESC);
