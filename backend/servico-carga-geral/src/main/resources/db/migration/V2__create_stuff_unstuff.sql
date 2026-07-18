CREATE TABLE operacao_stuff_unstuff (
    id UUID PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    conteiner_id VARCHAR(80) NOT NULL,
    armazem_id VARCHAR(80),
    posicao_operacao VARCHAR(120),
    equipe_recurso VARCHAR(120),
    lacre_inicial VARCHAR(80),
    lacre_final VARCHAR(80),
    motivo_cancelamento VARCHAR(1000),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    iniciado_em TIMESTAMP WITH TIME ZONE,
    concluido_em TIMESTAMP WITH TIME ZONE,
    cancelado_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT ck_operacao_stuff_unstuff_tipo CHECK (tipo IN ('STUFF', 'UNSTUFF')),
    CONSTRAINT ck_operacao_stuff_unstuff_status CHECK (
        status IN ('PLANEJADA', 'EM_EXECUCAO', 'PARCIAL', 'CONCLUIDA', 'CANCELADA')
    )
);

CREATE TABLE item_operacao_stuff_unstuff (
    id UUID PRIMARY KEY,
    operacao_id UUID NOT NULL,
    lote_id UUID NOT NULL,
    quantidade_planejada NUMERIC(19,3) NOT NULL,
    quantidade_realizada NUMERIC(19,3) NOT NULL DEFAULT 0,
    volume_planejado_m3 NUMERIC(19,3) NOT NULL,
    volume_realizado_m3 NUMERIC(19,3) NOT NULL DEFAULT 0,
    peso_planejado_kg NUMERIC(19,3) NOT NULL,
    peso_realizado_kg NUMERIC(19,3) NOT NULL DEFAULT 0,
    codigo_avaria VARCHAR(80),
    descricao_avaria VARCHAR(1000),
    divergencia VARCHAR(1000),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_item_stuff_unstuff_operacao FOREIGN KEY (operacao_id) REFERENCES operacao_stuff_unstuff(id),
    CONSTRAINT fk_item_stuff_unstuff_lote FOREIGN KEY (lote_id) REFERENCES lote_carga(id),
    CONSTRAINT uk_item_stuff_unstuff_operacao_lote UNIQUE (operacao_id, lote_id),
    CONSTRAINT ck_item_stuff_unstuff_planejado CHECK (
        quantidade_planejada > 0 AND volume_planejado_m3 >= 0 AND peso_planejado_kg >= 0
    ),
    CONSTRAINT ck_item_stuff_unstuff_realizado CHECK (
        quantidade_realizada >= 0 AND volume_realizado_m3 >= 0 AND peso_realizado_kg >= 0
        AND quantidade_realizada <= quantidade_planejada
        AND volume_realizado_m3 <= volume_planejado_m3
        AND peso_realizado_kg <= peso_planejado_kg
    )
);

CREATE TABLE evento_operacao_stuff_unstuff (
    id UUID PRIMARY KEY,
    operacao_id UUID NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    usuario VARCHAR(120) NOT NULL,
    correlation_id VARCHAR(120),
    descricao VARCHAR(1000),
    ocorrido_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_evento_stuff_unstuff_operacao FOREIGN KEY (operacao_id) REFERENCES operacao_stuff_unstuff(id)
);

CREATE INDEX idx_operacao_stuff_unstuff_status ON operacao_stuff_unstuff(status, criado_em DESC);
CREATE INDEX idx_operacao_stuff_unstuff_conteiner ON operacao_stuff_unstuff(conteiner_id, criado_em DESC);
CREATE INDEX idx_item_stuff_unstuff_lote ON item_operacao_stuff_unstuff(lote_id);
CREATE INDEX idx_evento_stuff_unstuff_operacao ON evento_operacao_stuff_unstuff(operacao_id, ocorrido_em);
