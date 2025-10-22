CREATE TABLE IF NOT EXISTS navio (
    identificador BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    codigo_imo VARCHAR(10) NOT NULL UNIQUE,
    pais_bandeira VARCHAR(60) NOT NULL,
    empresa_armadora VARCHAR(80) NOT NULL,
    capacidade_teu INTEGER NOT NULL CHECK (capacidade_teu > 0),
    status_operacao VARCHAR(30) NOT NULL,
    data_prevista_atracacao TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    data_efetiva_atracacao TIMESTAMP WITHOUT TIME ZONE,
    data_efetiva_desatracacao TIMESTAMP WITHOUT TIME ZONE,
    berco_previsto VARCHAR(20),
    berco_atual VARCHAR(20),
    observacoes VARCHAR(500)
);

CREATE INDEX IF NOT EXISTS idx_navio_status_operacao ON navio (status_operacao);
CREATE INDEX IF NOT EXISTS idx_navio_data_prevista ON navio (data_prevista_atracacao);
