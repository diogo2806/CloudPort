CREATE TABLE IF NOT EXISTS servico_linha (
    identificador BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    nome VARCHAR(120) NOT NULL,
    armador VARCHAR(80)
);

CREATE TABLE IF NOT EXISTS porto_rotacao (
    identificador BIGSERIAL PRIMARY KEY,
    servico_linha_id BIGINT NOT NULL REFERENCES servico_linha (identificador) ON DELETE CASCADE,
    sequencia INTEGER NOT NULL,
    porto_unloc VARCHAR(10) NOT NULL,
    nome_porto VARCHAR(80)
);

CREATE INDEX IF NOT EXISTS idx_porto_rotacao_servico ON porto_rotacao (servico_linha_id);

ALTER TABLE visita_navio ADD COLUMN IF NOT EXISTS servico_linha_id BIGINT REFERENCES servico_linha (identificador);
ALTER TABLE visita_navio ADD COLUMN IF NOT EXISTS chegada_prevista TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE visita_navio ADD COLUMN IF NOT EXISTS chegada_efetiva TIMESTAMP WITHOUT TIME ZONE;

CREATE INDEX IF NOT EXISTS idx_visita_navio_servico ON visita_navio (servico_linha_id);
