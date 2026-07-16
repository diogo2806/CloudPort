-- V6__idempotencia_eventos_visibilidade.sql
CREATE TABLE IF NOT EXISTS visibilidade_evento_processado (
    identidade_evento VARCHAR(150) PRIMARY KEY,
    tipo_evento VARCHAR(100) NOT NULL,
    hash_payload VARCHAR(64) NOT NULL,
    processado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_visibilidade_evento_processado_tipo
    ON visibilidade_evento_processado(tipo_evento);
CREATE INDEX IF NOT EXISTS idx_visibilidade_evento_processado_data
    ON visibilidade_evento_processado(processado_em);

ALTER TABLE historico_movimento
    ADD COLUMN IF NOT EXISTS evento_id VARCHAR(150);

CREATE UNIQUE INDEX IF NOT EXISTS uq_historico_movimento_evento_id
    ON historico_movimento(evento_id);
