CREATE TABLE comando_execucao_stuff_unstuff (
    id UUID PRIMARY KEY,
    operacao_id UUID NOT NULL,
    command_id UUID NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    aplicado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_comando_execucao_stuff_unstuff_operacao
        FOREIGN KEY (operacao_id) REFERENCES operacao_stuff_unstuff(id),
    CONSTRAINT uk_comando_execucao_stuff_unstuff
        UNIQUE (operacao_id, command_id)
);

CREATE INDEX idx_comando_execucao_stuff_unstuff_operacao
    ON comando_execucao_stuff_unstuff(operacao_id, aplicado_em DESC);
