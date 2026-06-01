-- V5__schema_yard_capacity.sql
CREATE TABLE IF NOT EXISTS capacidade_yard (
    id BIGSERIAL PRIMARY KEY,
    zona VARCHAR(50) UNIQUE,
    capacidade_total INTEGER,
    ocupacao_atual INTEGER,
    percentual_ocupacao DOUBLE PRECISION,
    equipamentos_disponiveis INTEGER,
    data_atualizacao TIMESTAMP
);

CREATE INDEX idx_capacidade_yard_zona ON capacidade_yard(zona);