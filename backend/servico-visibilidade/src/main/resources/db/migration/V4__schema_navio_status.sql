-- V4__schema_navio_status.sql
CREATE TABLE IF NOT EXISTS status_navio (
    id BIGSERIAL PRIMARY KEY,
    navio_id VARCHAR(50) UNIQUE NOT NULL,
    nome_navio VARCHAR(150),
    status_operacional VARCHAR(50),
    berco_alocado VARCHAR(50),
    eta_estimado TIMESTAMP,
    chegada_real TIMESTAMP,
    atraso_minutos INTEGER,
    porcentagem_completa DOUBLE PRECISION,
    data_atualizacao TIMESTAMP
);

CREATE INDEX idx_status_navio_navio_id ON status_navio(navio_id);
CREATE INDEX idx_status_navio_status ON status_navio(status_operacional);