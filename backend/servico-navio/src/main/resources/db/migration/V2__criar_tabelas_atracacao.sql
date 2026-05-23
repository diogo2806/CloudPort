CREATE TABLE IF NOT EXISTS berco (
    identificador BIGSERIAL PRIMARY KEY,
    nome VARCHAR(60) NOT NULL UNIQUE,
    comprimento_metros NUMERIC(8,2) NOT NULL CHECK (comprimento_metros > 0),
    calado_maximo_metros NUMERIC(6,2) NOT NULL CHECK (calado_maximo_metros > 0),
    status VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS visita_navio (
    identificador BIGSERIAL PRIMARY KEY,
    navio_id BIGINT NOT NULL REFERENCES navio (identificador),
    numero_viagem VARCHAR(40) NOT NULL,
    berco_id BIGINT REFERENCES berco (identificador),
    atracacao_prevista TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atracacao_efetiva TIMESTAMP WITHOUT TIME ZONE,
    desatracacao_prevista TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    desatracacao_efetiva TIMESTAMP WITHOUT TIME ZONE,
    status VARCHAR(30) NOT NULL,
    observacoes VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_visita_navio_berco ON visita_navio (berco_id);
CREATE INDEX IF NOT EXISTS idx_visita_navio_atracacao ON visita_navio (atracacao_prevista);
CREATE INDEX IF NOT EXISTS idx_visita_navio_navio ON visita_navio (navio_id);

CREATE TABLE IF NOT EXISTS operacao_navio_conteiner (
    identificador BIGSERIAL PRIMARY KEY,
    visita_id BIGINT NOT NULL REFERENCES visita_navio (identificador) ON DELETE CASCADE,
    tipo_operacao VARCHAR(20) NOT NULL,
    identificacao_conteiner VARCHAR(20) NOT NULL,
    bay INTEGER,
    fileira INTEGER,
    altura INTEGER,
    peso_toneladas NUMERIC(10,3),
    status VARCHAR(20) NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_operacao_navio_visita ON operacao_navio_conteiner (visita_id);
