CREATE TABLE plano_operacional_carga (
    id UUID PRIMARY KEY,
    numero VARCHAR(80) NOT NULL UNIQUE,
    tipo VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    prioridade INTEGER NOT NULL,
    versao_plano INTEGER NOT NULL DEFAULT 1,
    plano_origem_id UUID,
    janela_inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    janela_fim TIMESTAMP WITH TIME ZONE NOT NULL,
    local VARCHAR(120) NOT NULL,
    origem_tipo VARCHAR(30),
    origem_id VARCHAR(120),
    destino_tipo VARCHAR(30),
    destino_id VARCHAR(120),
    visita_navio_id VARCHAR(80),
    visita_ferroviaria_id VARCHAR(80),
    equipe_id VARCHAR(80),
    equipamento_id VARCHAR(80),
    lacre_origem VARCHAR(80),
    lacre_destino VARCHAR(80),
    restricoes VARCHAR(2000),
    instrucao_trabalho VARCHAR(2000),
    motivo_cancelamento VARCHAR(1000),
    historico_operacional VARCHAR(16000) NOT NULL DEFAULT '',
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    liberado_em TIMESTAMP WITH TIME ZONE,
    iniciado_em TIMESTAMP WITH TIME ZONE,
    concluido_em TIMESTAMP WITH TIME ZONE,
    cancelado_em TIMESTAMP WITH TIME ZONE,
    versao BIGINT,
    CONSTRAINT fk_plano_operacional_origem FOREIGN KEY (plano_origem_id) REFERENCES plano_operacional_carga(id),
    CONSTRAINT ck_plano_operacional_janela CHECK (janela_fim > janela_inicio),
    CONSTRAINT ck_plano_operacional_prioridade CHECK (prioridade BETWEEN 1 AND 999),
    CONSTRAINT ck_plano_operacional_versao CHECK (versao_plano > 0),
    CONSTRAINT ck_plano_operacional_status CHECK (
        status IN ('RASCUNHO','LIBERADO','EM_EXECUCAO','PARCIAL','CONCLUIDO','CANCELADO')
    )
);

CREATE TABLE item_plano_operacional_carga (
    id UUID PRIMARY KEY,
    plano_id UUID NOT NULL,
    lote_id UUID NOT NULL,
    sequencia INTEGER NOT NULL,
    quantidade_planejada NUMERIC(19,3) NOT NULL,
    volume_planejado_m3 NUMERIC(19,3) NOT NULL,
    peso_planejado_kg NUMERIC(19,3) NOT NULL,
    quantidade_realizada NUMERIC(19,3) NOT NULL DEFAULT 0,
    volume_realizado_m3 NUMERIC(19,3) NOT NULL DEFAULT 0,
    peso_realizado_kg NUMERIC(19,3) NOT NULL DEFAULT 0,
    posicao_planejada VARCHAR(120),
    posicao_origem_real VARCHAR(120),
    posicao_destino_real VARCHAR(120),
    area_porao VARCHAR(120),
    vagao_id VARCHAR(80),
    posicao_vagao VARCHAR(80),
    capacidade_reservada_kg NUMERIC(19,3),
    divergencia VARCHAR(1000),
    codigo_avaria VARCHAR(80),
    descricao_avaria VARCHAR(1000),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_item_plano_operacional_plano FOREIGN KEY (plano_id) REFERENCES plano_operacional_carga(id),
    CONSTRAINT fk_item_plano_operacional_lote FOREIGN KEY (lote_id) REFERENCES lote_carga(id),
    CONSTRAINT uk_item_plano_operacional_sequencia UNIQUE (plano_id, sequencia),
    CONSTRAINT uk_item_plano_operacional_lote UNIQUE (plano_id, lote_id),
    CONSTRAINT ck_item_plano_planejado CHECK (
        quantidade_planejada > 0 AND volume_planejado_m3 >= 0 AND peso_planejado_kg >= 0
    ),
    CONSTRAINT ck_item_plano_realizado CHECK (
        quantidade_realizada >= 0 AND volume_realizado_m3 >= 0 AND peso_realizado_kg >= 0
        AND quantidade_realizada <= quantidade_planejada
        AND volume_realizado_m3 <= volume_planejado_m3
        AND peso_realizado_kg <= peso_planejado_kg
    )
);

CREATE TABLE comando_plano_operacional_carga (
    id UUID PRIMARY KEY,
    plano_id UUID NOT NULL,
    command_id VARCHAR(120) NOT NULL,
    payload_hash VARCHAR(64) NOT NULL,
    usuario VARCHAR(120) NOT NULL,
    processado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_comando_plano_operacional FOREIGN KEY (plano_id) REFERENCES plano_operacional_carga(id),
    CONSTRAINT uk_comando_plano_command UNIQUE (plano_id, command_id)
);

CREATE TABLE avaria_operacional_carga (
    id UUID PRIMARY KEY,
    lote_id UUID NOT NULL,
    codigo VARCHAR(80) NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    quantidade_afetada NUMERIC(19,3) NOT NULL,
    volume_afetado_m3 NUMERIC(19,3) NOT NULL,
    peso_afetado_kg NUMERIC(19,3) NOT NULL,
    responsavel VARCHAR(120) NOT NULL,
    evidencias_json VARCHAR(8000),
    status VARCHAR(30) NOT NULL,
    relatorio_inspecao VARCHAR(4000),
    resultado_tratamento VARCHAR(30),
    observacao_encerramento VARCHAR(2000),
    historico_operacional VARCHAR(16000) NOT NULL DEFAULT '',
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    encerrado_em TIMESTAMP WITH TIME ZONE,
    versao BIGINT,
    CONSTRAINT fk_avaria_operacional_lote FOREIGN KEY (lote_id) REFERENCES lote_carga(id),
    CONSTRAINT ck_avaria_operacional_valores CHECK (
        quantidade_afetada > 0 AND volume_afetado_m3 >= 0 AND peso_afetado_kg >= 0
    ),
    CONSTRAINT ck_avaria_operacional_status CHECK (
        status IN ('ABERTA','SEGREGADA','EM_INSPECAO','EM_TRATAMENTO','REINTEGRADA','BAIXADA','BLOQUEADA','ENCERRADA')
    )
);

CREATE TABLE inventario_fisico_carga (
    id UUID PRIMARY KEY,
    codigo VARCHAR(80) NOT NULL UNIQUE,
    armazem_id VARCHAR(80) NOT NULL,
    posicao_referencia VARCHAR(120),
    motivo VARCHAR(1000) NOT NULL,
    status VARCHAR(30) NOT NULL,
    aberto_por VARCHAR(120) NOT NULL,
    aprovado_por VARCHAR(120),
    justificativa_ajuste VARCHAR(2000),
    historico_operacional VARCHAR(16000) NOT NULL DEFAULT '',
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    concluido_em TIMESTAMP WITH TIME ZONE,
    cancelado_em TIMESTAMP WITH TIME ZONE,
    versao BIGINT,
    CONSTRAINT ck_inventario_fisico_status CHECK (
        status IN ('ABERTO','EM_CONTAGEM','AGUARDANDO_APROVACAO','CONCLUIDO','CANCELADO')
    )
);

CREATE TABLE contagem_inventario_carga (
    id UUID PRIMARY KEY,
    inventario_id UUID NOT NULL,
    lote_id UUID NOT NULL,
    codigo_identificacao VARCHAR(160) NOT NULL,
    posicao VARCHAR(120) NOT NULL,
    numero_contagem INTEGER NOT NULL,
    quantidade NUMERIC(19,3) NOT NULL,
    volume_m3 NUMERIC(19,3) NOT NULL,
    peso_kg NUMERIC(19,3) NOT NULL,
    usuario VARCHAR(120) NOT NULL,
    correlation_id VARCHAR(120),
    contado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_contagem_inventario FOREIGN KEY (inventario_id) REFERENCES inventario_fisico_carga(id),
    CONSTRAINT fk_contagem_inventario_lote FOREIGN KEY (lote_id) REFERENCES lote_carga(id),
    CONSTRAINT uk_contagem_inventario_lote_numero UNIQUE (inventario_id, lote_id, numero_contagem),
    CONSTRAINT ck_contagem_inventario_valores CHECK (
        numero_contagem > 0 AND quantidade >= 0 AND volume_m3 >= 0 AND peso_kg >= 0
    )
);

CREATE INDEX idx_plano_operacional_status ON plano_operacional_carga(status, prioridade, janela_inicio);
CREATE INDEX idx_plano_operacional_navio ON plano_operacional_carga(visita_navio_id);
CREATE INDEX idx_plano_operacional_ferrovia ON plano_operacional_carga(visita_ferroviaria_id);
CREATE INDEX idx_item_plano_operacional_lote ON item_plano_operacional_carga(lote_id);
CREATE INDEX idx_avaria_operacional_lote_status ON avaria_operacional_carga(lote_id, status);
CREATE INDEX idx_inventario_fisico_status ON inventario_fisico_carga(status, armazem_id);
CREATE INDEX idx_contagem_inventario_lote ON contagem_inventario_carga(inventario_id, lote_id, contado_em);
