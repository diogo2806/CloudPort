CREATE TABLE operacao_stuff_unstuff (
    id UUID PRIMARY KEY,
    numero VARCHAR(80) NOT NULL UNIQUE,
    tipo VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    container_id VARCHAR(80) NOT NULL,
    armazem_id VARCHAR(80),
    posicao_operacao VARCHAR(120),
    equipe_recurso VARCHAR(160),
    lacre_inicial VARCHAR(80),
    lacre_final VARCHAR(80),
    usuario VARCHAR(120) NOT NULL,
    observacao VARCHAR(1000),
    motivo_cancelamento VARCHAR(1000),
    iniciada_em TIMESTAMP WITH TIME ZONE,
    concluida_em TIMESTAMP WITH TIME ZONE,
    cancelada_em TIMESTAMP WITH TIME ZONE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_operacao_stuff_unstuff_tipo CHECK (tipo IN ('STUFF', 'UNSTUFF')),
    CONSTRAINT ck_operacao_stuff_unstuff_status CHECK (status IN ('PLANEJADA', 'EM_EXECUCAO', 'PARCIAL', 'CONCLUIDA', 'CANCELADA'))
);

CREATE TABLE item_operacao_stuff_unstuff (
    id UUID PRIMARY KEY,
    operacao_id UUID NOT NULL,
    lote_id UUID NOT NULL,
    quantidade_planejada NUMERIC(19,3) NOT NULL,
    volume_planejado_m3 NUMERIC(19,3) NOT NULL,
    peso_planejado_kg NUMERIC(19,3) NOT NULL,
    quantidade_realizada NUMERIC(19,3) NOT NULL DEFAULT 0,
    volume_realizado_m3 NUMERIC(19,3) NOT NULL DEFAULT 0,
    peso_realizado_kg NUMERIC(19,3) NOT NULL DEFAULT 0,
    divergencia VARCHAR(1000),
    codigo_avaria VARCHAR(80),
    descricao_avaria VARCHAR(1000),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_item_operacao_stuff_unstuff_operacao FOREIGN KEY (operacao_id) REFERENCES operacao_stuff_unstuff(id),
    CONSTRAINT fk_item_operacao_stuff_unstuff_lote FOREIGN KEY (lote_id) REFERENCES lote_carga(id),
    CONSTRAINT uk_item_operacao_stuff_unstuff_lote UNIQUE (operacao_id, lote_id),
    CONSTRAINT ck_item_operacao_stuff_unstuff_planejado CHECK (
        quantidade_planejada >= 0 AND volume_planejado_m3 >= 0 AND peso_planejado_kg >= 0
        AND (quantidade_planejada > 0 OR volume_planejado_m3 > 0 OR peso_planejado_kg > 0)
    ),
    CONSTRAINT ck_item_operacao_stuff_unstuff_realizado CHECK (
        quantidade_realizada >= 0 AND volume_realizado_m3 >= 0 AND peso_realizado_kg >= 0
        AND quantidade_realizada <= quantidade_planejada
        AND volume_realizado_m3 <= volume_planejado_m3
        AND peso_realizado_kg <= peso_planejado_kg
    )
);

CREATE INDEX idx_operacao_stuff_unstuff_status ON operacao_stuff_unstuff(status, tipo);
CREATE INDEX idx_operacao_stuff_unstuff_container ON operacao_stuff_unstuff(container_id);
CREATE INDEX idx_item_operacao_stuff_unstuff_lote ON item_operacao_stuff_unstuff(lote_id);
