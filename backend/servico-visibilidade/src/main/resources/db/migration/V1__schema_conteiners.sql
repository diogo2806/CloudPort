-- V1__schema_conteiners.sql
CREATE TABLE IF NOT EXISTS conteiner_localizacao (
    id BIGSERIAL PRIMARY KEY,
    container_id VARCHAR(50) UNIQUE NOT NULL,
    status_atual VARCHAR(50),
    zona VARCHAR(50),
    posicao VARCHAR(100),
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    navio_destino_id VARCHAR(50),
    data_atualizacao TIMESTAMP
);

CREATE INDEX idx_conteiner_localizacao_container_id ON conteiner_localizacao(container_id);
CREATE INDEX idx_conteiner_localizacao_status ON conteiner_localizacao(status_atual);