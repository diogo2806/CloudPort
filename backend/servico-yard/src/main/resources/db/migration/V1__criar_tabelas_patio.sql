CREATE TABLE IF NOT EXISTS carga_patio (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    descricao VARCHAR(80) NOT NULL,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS posicao_patio (
    id BIGSERIAL PRIMARY KEY,
    linha INTEGER NOT NULL,
    coluna INTEGER NOT NULL,
    camada_operacional VARCHAR(40) NOT NULL,
    CONSTRAINT uk_posicao_unica UNIQUE (linha, coluna, camada_operacional)
);

CREATE TABLE IF NOT EXISTS conteiner_patio (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    status_conteiner VARCHAR(30) NOT NULL,
    destino VARCHAR(60) NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    posicao_id BIGINT NOT NULL REFERENCES posicao_patio(id),
    carga_id BIGINT NOT NULL REFERENCES carga_patio(id)
);

CREATE TABLE IF NOT EXISTS equipamento_patio (
    id BIGSERIAL PRIMARY KEY,
    identificador VARCHAR(30) NOT NULL UNIQUE,
    tipo_equipamento VARCHAR(30) NOT NULL,
    linha INTEGER NOT NULL,
    coluna INTEGER NOT NULL,
    status_operacional VARCHAR(30) NOT NULL
);

CREATE TABLE IF NOT EXISTS movimento_patio (
    id BIGSERIAL PRIMARY KEY,
    conteiner_id BIGINT NOT NULL REFERENCES conteiner_patio(id) ON DELETE CASCADE,
    tipo_movimento VARCHAR(30) NOT NULL,
    descricao VARCHAR(160) NOT NULL,
    registrado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_posicao_linha_coluna ON posicao_patio (linha, coluna);
CREATE INDEX IF NOT EXISTS idx_movimento_conteiner ON movimento_patio (conteiner_id, registrado_em DESC);
