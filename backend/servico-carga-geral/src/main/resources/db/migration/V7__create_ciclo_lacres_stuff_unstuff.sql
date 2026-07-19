CREATE TABLE lacre_operacao_stuff_unstuff (
    id UUID PRIMARY KEY,
    operacao_id UUID NOT NULL,
    command_id UUID NOT NULL,
    numero_lacre VARCHAR(80) NOT NULL,
    numero_lacre_substituido VARCHAR(80),
    tipo_evento VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    operador VARCHAR(120) NOT NULL,
    correlation_id VARCHAR(120),
    motivo VARCHAR(1000),
    divergencia_aberta BOOLEAN NOT NULL DEFAULT FALSE,
    override_autorizado BOOLEAN NOT NULL DEFAULT FALSE,
    ocorrido_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_lacre_operacao_stuff_unstuff
        FOREIGN KEY (operacao_id) REFERENCES operacao_stuff_unstuff(id),
    CONSTRAINT uk_lacre_operacao_command UNIQUE (operacao_id, command_id),
    CONSTRAINT ck_lacre_tipo_evento
        CHECK (tipo_evento IN ('PREVISTO', 'APLICADO', 'ROMPIDO', 'SUBSTITUIDO', 'CONFERIDO')),
    CONSTRAINT ck_lacre_status
        CHECK (status IN ('PREVISTO', 'APLICADO', 'ROMPIDO', 'SUBSTITUIDO', 'CONFERIDO', 'DIVERGENTE'))
);

CREATE INDEX idx_lacre_operacao_ocorrido
    ON lacre_operacao_stuff_unstuff (operacao_id, ocorrido_em);

CREATE INDEX idx_lacre_divergencia_aberta
    ON lacre_operacao_stuff_unstuff (operacao_id, divergencia_aberta)
    WHERE divergencia_aberta = TRUE;
