CREATE TABLE IF NOT EXISTS pessoa_acesso (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(140) NOT NULL,
    documento VARCHAR(30) NOT NULL,
    documento_normalizado VARCHAR(30) NOT NULL,
    tipo_pessoa VARCHAR(30) NOT NULL,
    empresa VARCHAR(140),
    cracha VARCHAR(50),
    situacao VARCHAR(20) NOT NULL,
    ultimo_acesso_em TIMESTAMP NOT NULL,
    ultimo_ponto_acesso VARCHAR(120) NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_pessoa_acesso_documento_normalizado UNIQUE (documento_normalizado),
    CONSTRAINT ck_pessoa_acesso_situacao CHECK (situacao IN ('FORA', 'DENTRO'))
);

CREATE TABLE IF NOT EXISTS movimentacao_pessoa_acesso (
    id BIGSERIAL PRIMARY KEY,
    pessoa_acesso_id BIGINT NOT NULL,
    direcao VARCHAR(20) NOT NULL,
    ponto_acesso VARCHAR(120) NOT NULL,
    motivo VARCHAR(500),
    registrado_em TIMESTAMP NOT NULL,
    usuario_responsavel VARCHAR(120) NOT NULL,
    origem_acao VARCHAR(80),
    correlation_id VARCHAR(100),
    permanencia_minutos BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_movimentacao_pessoa_acesso_pessoa
        FOREIGN KEY (pessoa_acesso_id) REFERENCES pessoa_acesso (id),
    CONSTRAINT ck_movimentacao_pessoa_direcao CHECK (direcao IN ('ENTRADA', 'SAIDA')),
    CONSTRAINT ck_movimentacao_pessoa_permanencia CHECK (permanencia_minutos IS NULL OR permanencia_minutos >= 0)
);

CREATE INDEX IF NOT EXISTS idx_pessoa_acesso_situacao
    ON pessoa_acesso (situacao, ultimo_acesso_em);

CREATE INDEX IF NOT EXISTS idx_movimentacao_pessoa_documento_data
    ON movimentacao_pessoa_acesso (pessoa_acesso_id, registrado_em DESC);

CREATE INDEX IF NOT EXISTS idx_movimentacao_pessoa_data
    ON movimentacao_pessoa_acesso (registrado_em DESC);
