-- V3__schema_alertas.sql
CREATE TABLE IF NOT EXISTS alerta (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(50),
    severidade VARCHAR(20),
    entidade_id VARCHAR(100),
    descricao TEXT,
    data_gerada TIMESTAMP,
    data_resolucao TIMESTAMP,
    status VARCHAR(20),
    acao_sugerida TEXT
);

CREATE INDEX idx_alerta_tipo ON alerta(tipo);
CREATE INDEX idx_alerta_severidade ON alerta(severidade);
CREATE INDEX idx_alerta_status ON alerta(status);