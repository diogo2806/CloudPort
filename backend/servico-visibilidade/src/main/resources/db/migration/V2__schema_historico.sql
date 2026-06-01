-- V2__schema_historico.sql
CREATE TABLE IF NOT EXISTS historico_movimento (
    id BIGSERIAL PRIMARY KEY,
    container_id VARCHAR(50) NOT NULL,
    timestamp TIMESTAMP,
    tipo VARCHAR(50),
    localizacao VARCHAR(200),
    responsavel VARCHAR(100),
    observacoes TEXT,
    equipamento_usado VARCHAR(100)
);

CREATE INDEX idx_historico_container_id ON historico_movimento(container_id);
CREATE INDEX idx_historico_timestamp ON historico_movimento(timestamp);