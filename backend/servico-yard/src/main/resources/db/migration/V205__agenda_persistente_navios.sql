CREATE TABLE IF NOT EXISTS vessel_schedule (
    id BIGSERIAL PRIMARY KEY,
    codigo_navio VARCHAR(80) NOT NULL,
    nome_berco VARCHAR(80) NOT NULL,
    tempo_previsto TIMESTAMP NOT NULL,
    tempo_termino TIMESTAMP NOT NULL,
    prioridade VARCHAR(30) NOT NULL,
    capacidade_requerida INTEGER NOT NULL,
    criado_em TIMESTAMP NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_vessel_schedule_berco_janela
    ON vessel_schedule (nome_berco, tempo_previsto, tempo_termino);
