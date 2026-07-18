ALTER TABLE lote_carga
    ADD COLUMN quantidade_bloqueada NUMERIC(19,3) NOT NULL DEFAULT 0,
    ADD COLUMN volume_bloqueado_m3 NUMERIC(19,3) NOT NULL DEFAULT 0,
    ADD COLUMN peso_bloqueado_kg NUMERIC(19,3) NOT NULL DEFAULT 0;

CREATE TABLE operacao_transload (
    id UUID PRIMARY KEY,
    command_id UUID NOT NULL UNIQUE,
    unidade_origem VARCHAR(80) NOT NULL,
    unidade_destino VARCHAR(80) NOT NULL,
    reserva_origem_id UUID NOT NULL,
    reserva_destino_id UUID NOT NULL,
    lacre_origem VARCHAR(80),
    lacre_destino VARCHAR(80),
    divergencia VARCHAR(1000),
    codigo_avaria VARCHAR(80),
    descricao_avaria VARCHAR(1000),
    status VARCHAR(20) NOT NULL,
    usuario VARCHAR(120) NOT NULL,
    correlation_id VARCHAR(120),
    executado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_operacao_transload_unidades CHECK (unidade_origem <> unidade_destino),
    CONSTRAINT ck_operacao_transload_status CHECK (status IN ('CONCLUIDO', 'CANCELADO'))
);

CREATE TABLE item_operacao_transload (
    operacao_id UUID NOT NULL,
    ordem INTEGER NOT NULL,
    lote_origem_id UUID NOT NULL,
    lote_destino_id UUID NOT NULL,
    quantidade NUMERIC(19,3) NOT NULL,
    volume_m3 NUMERIC(19,3) NOT NULL,
    peso_kg NUMERIC(19,3) NOT NULL,
    PRIMARY KEY (operacao_id, ordem),
    CONSTRAINT fk_item_transload_operacao FOREIGN KEY (operacao_id) REFERENCES operacao_transload(id),
    CONSTRAINT fk_item_transload_lote_origem FOREIGN KEY (lote_origem_id) REFERENCES lote_carga(id),
    CONSTRAINT fk_item_transload_lote_destino FOREIGN KEY (lote_destino_id) REFERENCES lote_carga(id),
    CONSTRAINT ck_item_transload_lotes CHECK (lote_origem_id <> lote_destino_id),
    CONSTRAINT ck_item_transload_quantidade CHECK (quantidade > 0 AND volume_m3 >= 0 AND peso_kg >= 0)
);

CREATE TABLE reserva_gate_carga (
    id UUID PRIMARY KEY,
    command_id_reserva UUID NOT NULL UNIQUE,
    command_id_confirmacao UUID UNIQUE,
    command_id_compensacao UUID UNIQUE,
    agendamento_codigo VARCHAR(80) NOT NULL,
    bl_numero VARCHAR(100) NOT NULL,
    delivery_order VARCHAR(100) NOT NULL,
    lote_id UUID NOT NULL,
    tipo_movimento VARCHAR(20) NOT NULL,
    estagio_confirmacao VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    quantidade NUMERIC(19,3) NOT NULL,
    volume_m3 NUMERIC(19,3) NOT NULL,
    peso_kg NUMERIC(19,3) NOT NULL,
    usuario_reserva VARCHAR(120) NOT NULL,
    reservado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    usuario_confirmacao VARCHAR(120),
    confirmado_em TIMESTAMP WITH TIME ZONE,
    motivo_compensacao VARCHAR(1000),
    compensado_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_reserva_gate_carga_lote FOREIGN KEY (lote_id) REFERENCES lote_carga(id),
    CONSTRAINT ck_reserva_gate_carga_tipo CHECK (tipo_movimento IN ('ENTREGA', 'RETIRADA')),
    CONSTRAINT ck_reserva_gate_carga_estagio CHECK (estagio_confirmacao IN ('ENTRADA', 'SAIDA')),
    CONSTRAINT ck_reserva_gate_carga_status CHECK (status IN ('RESERVADA', 'CONFIRMADA', 'COMPENSADA')),
    CONSTRAINT ck_reserva_gate_carga_quantidade CHECK (quantidade > 0 AND volume_m3 >= 0 AND peso_kg >= 0)
);

CREATE INDEX idx_reserva_gate_carga_agendamento
    ON reserva_gate_carga(agendamento_codigo, reservado_em DESC);
CREATE INDEX idx_reserva_gate_carga_lote_status
    ON reserva_gate_carga(lote_id, tipo_movimento, status);

CREATE TABLE alocacao_cargo_lot (
    id UUID PRIMARY KEY,
    command_id UUID NOT NULL UNIQUE,
    lote_id UUID NOT NULL,
    reserva_capacidade_id UUID NOT NULL,
    origem VARCHAR(120),
    destino VARCHAR(120) NOT NULL,
    recurso VARCHAR(120) NOT NULL,
    prioridade INTEGER NOT NULL,
    restricoes VARCHAR(1000),
    quantidade NUMERIC(19,3) NOT NULL,
    volume_m3 NUMERIC(19,3) NOT NULL,
    peso_kg NUMERIC(19,3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    usuario VARCHAR(120) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    confirmado_em TIMESTAMP WITH TIME ZONE,
    motivo_cancelamento VARCHAR(1000),
    CONSTRAINT fk_alocacao_cargo_lot_lote FOREIGN KEY (lote_id) REFERENCES lote_carga(id),
    CONSTRAINT ck_alocacao_cargo_lot_status CHECK (status IN ('PLANEJADA', 'RESERVADA', 'CONFIRMADA', 'CANCELADA')),
    CONSTRAINT ck_alocacao_cargo_lot_prioridade CHECK (prioridade BETWEEN 1 AND 999),
    CONSTRAINT ck_alocacao_cargo_lot_quantidade CHECK (quantidade > 0 AND volume_m3 >= 0 AND peso_kg >= 0)
);

CREATE INDEX idx_alocacao_cargo_lot_lote
    ON alocacao_cargo_lot(lote_id, criado_em DESC);

CREATE TABLE plano_transporte_cargo_lot (
    id UUID PRIMARY KEY,
    command_id_planejamento UUID NOT NULL UNIQUE,
    command_id_execucao UUID UNIQUE,
    modal VARCHAR(20) NOT NULL,
    tipo_operacao VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    visita_id VARCHAR(80) NOT NULL,
    bl_numero VARCHAR(100) NOT NULL,
    lote_id UUID NOT NULL,
    compartimento VARCHAR(120) NOT NULL,
    posicao VARCHAR(120),
    sequencia INTEGER NOT NULL,
    equipamento VARCHAR(120) NOT NULL,
    custodia VARCHAR(120),
    restricoes VARCHAR(1000),
    capacidade_peso_kg NUMERIC(19,3),
    quantidade_planejada NUMERIC(19,3) NOT NULL,
    volume_planejado_m3 NUMERIC(19,3) NOT NULL,
    peso_planejado_kg NUMERIC(19,3) NOT NULL,
    quantidade_realizada NUMERIC(19,3) NOT NULL,
    volume_realizado_m3 NUMERIC(19,3) NOT NULL,
    peso_realizado_kg NUMERIC(19,3) NOT NULL,
    usuario_planejamento VARCHAR(120) NOT NULL,
    planejado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    usuario_execucao VARCHAR(120),
    executado_em TIMESTAMP WITH TIME ZONE,
    motivo_cancelamento VARCHAR(1000),
    CONSTRAINT fk_plano_transporte_cargo_lote FOREIGN KEY (lote_id) REFERENCES lote_carga(id),
    CONSTRAINT uk_plano_transporte_cargo UNIQUE (modal, visita_id, lote_id, sequencia),
    CONSTRAINT ck_plano_transporte_cargo_modal CHECK (modal IN ('NAVIO', 'FERROVIA')),
    CONSTRAINT ck_plano_transporte_cargo_tipo CHECK (tipo_operacao IN ('CARGA', 'DESCARGA')),
    CONSTRAINT ck_plano_transporte_cargo_status CHECK (status IN ('PLANEJADO', 'EM_EXECUCAO', 'CONCLUIDO', 'CANCELADO')),
    CONSTRAINT ck_plano_transporte_cargo_sequencia CHECK (sequencia > 0),
    CONSTRAINT ck_plano_transporte_cargo_quantidade CHECK (
        quantidade_planejada > 0 AND volume_planejado_m3 >= 0 AND peso_planejado_kg >= 0
        AND quantidade_realizada >= 0 AND volume_realizado_m3 >= 0 AND peso_realizado_kg >= 0)
);

CREATE INDEX idx_plano_transporte_cargo_visita
    ON plano_transporte_cargo_lot(modal, visita_id, sequencia);

CREATE TABLE avaria_carga (
    id UUID PRIMARY KEY,
    command_id UUID NOT NULL UNIQUE,
    lote_id UUID NOT NULL,
    codigo VARCHAR(80) NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    quantidade_afetada NUMERIC(19,3) NOT NULL,
    volume_afetado_m3 NUMERIC(19,3) NOT NULL,
    peso_afetado_kg NUMERIC(19,3) NOT NULL,
    status VARCHAR(30) NOT NULL,
    responsavel VARCHAR(120) NOT NULL,
    inspecionado_por VARCHAR(120),
    reparado_por VARCHAR(120),
    observacoes VARCHAR(1000),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_avaria_carga_lote FOREIGN KEY (lote_id) REFERENCES lote_carga(id),
    CONSTRAINT ck_avaria_carga_status CHECK (status IN ('ABERTA', 'BLOQUEADA', 'EM_INSPECAO', 'EM_REPARO', 'REPARADA', 'BAIXADA')),
    CONSTRAINT ck_avaria_carga_quantidade CHECK (quantidade_afetada > 0 AND volume_afetado_m3 >= 0 AND peso_afetado_kg >= 0)
);

CREATE TABLE evidencia_avaria_carga (
    avaria_id UUID NOT NULL,
    ordem INTEGER NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    uri VARCHAR(1000) NOT NULL,
    checksum VARCHAR(128),
    responsavel VARCHAR(120) NOT NULL,
    registrado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    PRIMARY KEY (avaria_id, ordem),
    CONSTRAINT fk_evidencia_avaria_carga FOREIGN KEY (avaria_id) REFERENCES avaria_carga(id)
);

CREATE INDEX idx_avaria_carga_lote
    ON avaria_carga(lote_id, criado_em DESC);

CREATE TABLE inventario_fisico_cargo_lot (
    id UUID PRIMARY KEY,
    command_id_abertura UUID NOT NULL UNIQUE,
    posicao VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    aberto_por VARCHAR(120) NOT NULL,
    aberto_em TIMESTAMP WITH TIME ZONE NOT NULL,
    concluido_por VARCHAR(120),
    concluido_em TIMESTAMP WITH TIME ZONE,
    motivo VARCHAR(1000),
    CONSTRAINT ck_inventario_fisico_cargo_status CHECK (status IN ('ABERTO', 'EM_CONTAGEM', 'AGUARDANDO_APROVACAO', 'CONCLUIDO', 'CANCELADO'))
);

CREATE TABLE contagem_inventario_cargo_lot (
    inventario_id UUID NOT NULL,
    ordem INTEGER NOT NULL,
    command_id UUID NOT NULL,
    lote_id UUID NOT NULL,
    identificacao VARCHAR(160) NOT NULL,
    quantidade_logica NUMERIC(19,3) NOT NULL,
    volume_logico_m3 NUMERIC(19,3) NOT NULL,
    peso_logico_kg NUMERIC(19,3) NOT NULL,
    quantidade_contada NUMERIC(19,3) NOT NULL,
    volume_contado_m3 NUMERIC(19,3) NOT NULL,
    peso_contado_kg NUMERIC(19,3) NOT NULL,
    status_divergencia VARCHAR(30) NOT NULL,
    usuario VARCHAR(120) NOT NULL,
    observacao VARCHAR(1000),
    contado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    resolvido_por VARCHAR(120),
    motivo_resolucao VARCHAR(1000),
    PRIMARY KEY (inventario_id, ordem),
    CONSTRAINT fk_contagem_inventario_cargo FOREIGN KEY (inventario_id) REFERENCES inventario_fisico_cargo_lot(id),
    CONSTRAINT fk_contagem_inventario_cargo_lote FOREIGN KEY (lote_id) REFERENCES lote_carga(id),
    CONSTRAINT ck_contagem_inventario_status CHECK (status_divergencia IN ('SEM_DIVERGENCIA', 'PENDENTE', 'AJUSTADA', 'REJEITADA')),
    CONSTRAINT ck_contagem_inventario_valores CHECK (
        quantidade_logica >= 0 AND volume_logico_m3 >= 0 AND peso_logico_kg >= 0
        AND quantidade_contada >= 0 AND volume_contado_m3 >= 0 AND peso_contado_kg >= 0)
);

CREATE UNIQUE INDEX uk_contagem_inventario_cargo_command
    ON contagem_inventario_cargo_lot(command_id);
CREATE INDEX idx_inventario_fisico_cargo_posicao
    ON inventario_fisico_cargo_lot(posicao, aberto_em DESC);
