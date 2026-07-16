CREATE TABLE evento_consumido (
    evento_id VARCHAR(150) PRIMARY KEY,
    origem VARCHAR(40) NOT NULL,
    tipo_evento VARCHAR(120) NOT NULL,
    hash_payload VARCHAR(64) NOT NULL,
    processado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

ALTER TABLE historico_movimento
    ADD COLUMN evento_id VARCHAR(150);

CREATE UNIQUE INDEX uq_historico_movimento_evento_id
    ON historico_movimento(evento_id)
    WHERE evento_id IS NOT NULL;

CREATE INDEX idx_evento_consumido_processado_em
    ON evento_consumido(processado_em);
