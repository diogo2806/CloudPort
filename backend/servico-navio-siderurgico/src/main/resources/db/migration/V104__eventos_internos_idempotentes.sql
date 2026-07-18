CREATE TABLE evento_interno_processado (
    evento_id VARCHAR(36) PRIMARY KEY,
    tipo_evento VARCHAR(120) NOT NULL,
    processado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_evento_interno_processado_em
    ON evento_interno_processado(processado_em);
