CREATE TABLE IF NOT EXISTS evento_processado (
    id BIGSERIAL PRIMARY KEY,
    identidade_evento VARCHAR(150) NOT NULL,
    tipo_evento VARCHAR(150) NOT NULL,
    versao_evento INTEGER NOT NULL,
    consumidor VARCHAR(50) NOT NULL,
    origem_evento VARCHAR(100),
    hash_payload VARCHAR(64) NOT NULL,
    processado_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_evento_processado_identidade UNIQUE (identidade_evento),
    CONSTRAINT ck_evento_processado_versao CHECK (versao_evento > 0)
);

CREATE INDEX IF NOT EXISTS idx_evento_processado_tipo
    ON evento_processado(tipo_evento);

CREATE INDEX IF NOT EXISTS idx_evento_processado_processado_em
    ON evento_processado(processado_em);

ALTER TABLE historico_movimento
    ADD COLUMN IF NOT EXISTS evento_id VARCHAR(150);

CREATE UNIQUE INDEX IF NOT EXISTS uk_historico_movimento_evento_id
    ON historico_movimento(evento_id);
