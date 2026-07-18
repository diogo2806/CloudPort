CREATE TABLE plano_stuff_unstuff_versao (
    id UUID PRIMARY KEY,
    operacao_id UUID NOT NULL,
    numero_versao INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    criado_por VARCHAR(120) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    liberado_por VARCHAR(120),
    liberado_em TIMESTAMP WITH TIME ZONE,
    motivo VARCHAR(1000),
    CONSTRAINT fk_plano_stuff_unstuff_operacao
        FOREIGN KEY (operacao_id) REFERENCES operacao_stuff_unstuff(id),
    CONSTRAINT uk_plano_stuff_unstuff_versao UNIQUE (operacao_id, numero_versao),
    CONSTRAINT ck_plano_stuff_unstuff_status
        CHECK (status IN ('RASCUNHO', 'LIBERADO', 'SUPERADO', 'CANCELADO'))
);

CREATE TABLE plano_stuff_unstuff_item (
    plano_id UUID NOT NULL,
    ordem INTEGER NOT NULL,
    lote_id UUID NOT NULL,
    quantidade_planejada NUMERIC(19,3) NOT NULL,
    volume_planejado_m3 NUMERIC(19,3) NOT NULL,
    peso_planejado_kg NUMERIC(19,3) NOT NULL,
    PRIMARY KEY (plano_id, ordem),
    CONSTRAINT fk_plano_stuff_unstuff_item_plano
        FOREIGN KEY (plano_id) REFERENCES plano_stuff_unstuff_versao(id),
    CONSTRAINT fk_plano_stuff_unstuff_item_lote
        FOREIGN KEY (lote_id) REFERENCES lote_carga(id)
);

CREATE INDEX idx_plano_stuff_unstuff_operacao
    ON plano_stuff_unstuff_versao(operacao_id, numero_versao DESC);
