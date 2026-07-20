CREATE TABLE plano_posicao_operacional (
    id BIGSERIAL PRIMARY KEY,
    codigo_container VARCHAR(30) NOT NULL,
    ordem_trabalho_patio_id BIGINT REFERENCES ordem_trabalho_patio(id),
    bloco VARCHAR(40),
    linha INTEGER NOT NULL,
    coluna INTEGER NOT NULL,
    camada VARCHAR(40) NOT NULL,
    equipamento_id VARCHAR(80),
    estado VARCHAR(20) NOT NULL,
    horizonte_inicio TIMESTAMP NOT NULL,
    horizonte_fim TIMESTAMP NOT NULL,
    valido_ate TIMESTAMP NOT NULL,
    origem VARCHAR(80) NOT NULL,
    motivo VARCHAR(1000) NOT NULL,
    assinatura_entrada VARCHAR(128) NOT NULL,
    alterado_por VARCHAR(120) NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    convertido_em TIMESTAMP,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_plano_posicao_assinatura_unidade UNIQUE (assinatura_entrada, codigo_container),
    CONSTRAINT ck_plano_posicao_estado CHECK (
        estado IN ('TENTATIVO', 'DEFINITIVO', 'IMINENTE', 'EXPIRADO', 'CANCELADO')
    ),
    CONSTRAINT ck_plano_posicao_horizonte CHECK (horizonte_fim > horizonte_inicio)
);

CREATE INDEX idx_plano_posicao_estado_validade
    ON plano_posicao_operacional (estado, valido_ate);

CREATE INDEX idx_plano_posicao_bloco_horizonte
    ON plano_posicao_operacional (bloco, horizonte_inicio, horizonte_fim);

CREATE INDEX idx_plano_posicao_unidade
    ON plano_posicao_operacional (codigo_container, atualizado_em DESC);

CREATE TABLE historico_plano_posicao_operacional (
    id BIGSERIAL PRIMARY KEY,
    plano_id BIGINT NOT NULL REFERENCES plano_posicao_operacional(id),
    estado_anterior VARCHAR(20),
    estado_novo VARCHAR(20) NOT NULL,
    motivo VARCHAR(1000) NOT NULL,
    operador VARCHAR(120) NOT NULL,
    versao_plano BIGINT NOT NULL,
    ocorrido_em TIMESTAMP NOT NULL,
    CONSTRAINT ck_historico_plano_estado_anterior CHECK (
        estado_anterior IS NULL OR estado_anterior IN ('TENTATIVO', 'DEFINITIVO', 'IMINENTE', 'EXPIRADO', 'CANCELADO')
    ),
    CONSTRAINT ck_historico_plano_estado_novo CHECK (
        estado_novo IN ('TENTATIVO', 'DEFINITIVO', 'IMINENTE', 'EXPIRADO', 'CANCELADO')
    )
);

CREATE INDEX idx_historico_plano_posicao
    ON historico_plano_posicao_operacional (plano_id, ocorrido_em DESC);
