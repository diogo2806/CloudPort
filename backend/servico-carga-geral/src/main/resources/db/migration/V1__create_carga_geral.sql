CREATE TABLE referencia_carga (
    id UUID PRIMARY KEY,
    categoria VARCHAR(40) NOT NULL,
    codigo VARCHAR(80) NOT NULL,
    descricao VARCHAR(240) NOT NULL,
    atributos_json TEXT,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_referencia_carga_categoria_codigo UNIQUE (categoria, codigo)
);

CREATE TABLE conhecimento_carga (
    id UUID PRIMARY KEY,
    numero VARCHAR(80) NOT NULL UNIQUE,
    tipo_operacao VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    embarcador VARCHAR(180) NOT NULL,
    consignatario VARCHAR(180) NOT NULL,
    cliente_id VARCHAR(80),
    operador_id VARCHAR(80),
    visita_navio_id VARCHAR(80),
    visita_veiculo_id VARCHAR(80),
    armazem_id VARCHAR(80),
    porto_origem VARCHAR(120),
    porto_destino VARCHAR(120),
    observacoes VARCHAR(1000),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE TABLE item_conhecimento_carga (
    id UUID PRIMARY KEY,
    conhecimento_id UUID NOT NULL,
    sequencia INTEGER NOT NULL,
    descricao VARCHAR(300) NOT NULL,
    commodity_codigo VARCHAR(80) NOT NULL,
    tipo_produto_codigo VARCHAR(80) NOT NULL,
    tipo_embalagem_codigo VARCHAR(80) NOT NULL,
    quantidade_manifestada NUMERIC(19,3) NOT NULL,
    volume_m3 NUMERIC(19,3) NOT NULL,
    peso_kg NUMERIC(19,3) NOT NULL,
    codigo_armazenagem VARCHAR(80),
    codigo_manuseio VARCHAR(80),
    mercadoria_perigosa BOOLEAN NOT NULL DEFAULT FALSE,
    numero_un VARCHAR(20),
    classe_imdg VARCHAR(20),
    temperatura_minima NUMERIC(8,2),
    temperatura_maxima NUMERIC(8,2),
    CONSTRAINT fk_item_conhecimento_carga_conhecimento
        FOREIGN KEY (conhecimento_id) REFERENCES conhecimento_carga(id),
    CONSTRAINT uk_item_conhecimento_sequencia UNIQUE (conhecimento_id, sequencia),
    CONSTRAINT ck_item_carga_quantidades CHECK (
        quantidade_manifestada > 0 AND volume_m3 >= 0 AND peso_kg >= 0
    ),
    CONSTRAINT ck_item_carga_temperatura CHECK (
        temperatura_minima IS NULL OR temperatura_maxima IS NULL OR temperatura_minima <= temperatura_maxima
    ),
    CONSTRAINT ck_item_carga_perigosa CHECK (
        mercadoria_perigosa = FALSE OR (numero_un IS NOT NULL AND classe_imdg IS NOT NULL)
    )
);

CREATE TABLE lote_carga (
    id UUID PRIMARY KEY,
    codigo VARCHAR(100) NOT NULL UNIQUE,
    item_id UUID NOT NULL,
    lote_pai_id UUID,
    natureza VARCHAR(30) NOT NULL,
    status VARCHAR(40) NOT NULL,
    quantidade_prevista NUMERIC(19,3) NOT NULL,
    volume_previsto_m3 NUMERIC(19,3) NOT NULL,
    peso_previsto_kg NUMERIC(19,3) NOT NULL,
    quantidade_saldo NUMERIC(19,3) NOT NULL DEFAULT 0,
    volume_saldo_m3 NUMERIC(19,3) NOT NULL DEFAULT 0,
    peso_saldo_kg NUMERIC(19,3) NOT NULL DEFAULT 0,
    unidade_medida VARCHAR(20) NOT NULL,
    marcas_embalagem VARCHAR(300),
    armazem_id VARCHAR(80),
    posicao_armazenagem VARCHAR(120),
    veiculo_id VARCHAR(80),
    visita_navio_id VARCHAR(80),
    cliente_id VARCHAR(80),
    codigo_avaria VARCHAR(80),
    descricao_avaria VARCHAR(1000),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_lote_carga_item FOREIGN KEY (item_id) REFERENCES item_conhecimento_carga(id),
    CONSTRAINT fk_lote_carga_pai FOREIGN KEY (lote_pai_id) REFERENCES lote_carga(id),
    CONSTRAINT ck_lote_carga_previsto CHECK (
        quantidade_prevista > 0 AND volume_previsto_m3 >= 0 AND peso_previsto_kg >= 0
    ),
    CONSTRAINT ck_lote_carga_saldo CHECK (
        quantidade_saldo >= 0 AND volume_saldo_m3 >= 0 AND peso_saldo_kg >= 0
    )
);

CREATE TABLE movimentacao_carga (
    id UUID PRIMARY KEY,
    lote_id UUID NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    quantidade NUMERIC(19,3) NOT NULL,
    volume_m3 NUMERIC(19,3) NOT NULL,
    peso_kg NUMERIC(19,3) NOT NULL,
    lote_relacionado_id UUID,
    origem_tipo VARCHAR(40),
    origem_id VARCHAR(120),
    destino_tipo VARCHAR(40),
    destino_id VARCHAR(120),
    veiculo_id VARCHAR(80),
    visita_navio_id VARCHAR(80),
    armazem_id VARCHAR(80),
    cliente_id VARCHAR(80),
    usuario VARCHAR(120) NOT NULL,
    correlation_id VARCHAR(120),
    observacao VARCHAR(1000),
    ocorrido_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_movimentacao_carga_lote FOREIGN KEY (lote_id) REFERENCES lote_carga(id),
    CONSTRAINT fk_movimentacao_carga_lote_relacionado FOREIGN KEY (lote_relacionado_id) REFERENCES lote_carga(id),
    CONSTRAINT ck_movimentacao_carga_valores CHECK (
        quantidade >= 0 AND volume_m3 >= 0 AND peso_kg >= 0
    )
);

CREATE INDEX idx_conhecimento_carga_status ON conhecimento_carga(status);
CREATE INDEX idx_conhecimento_carga_visita_navio ON conhecimento_carga(visita_navio_id);
CREATE INDEX idx_lote_carga_status_natureza ON lote_carga(status, natureza);
CREATE INDEX idx_lote_carga_armazem ON lote_carga(armazem_id, posicao_armazenagem);
CREATE INDEX idx_lote_carga_visita_navio ON lote_carga(visita_navio_id);
CREATE INDEX idx_lote_carga_veiculo ON lote_carga(veiculo_id);
CREATE INDEX idx_movimentacao_carga_lote_data ON movimentacao_carga(lote_id, ocorrido_em DESC);
CREATE INDEX idx_movimentacao_carga_correlation ON movimentacao_carga(correlation_id);

INSERT INTO referencia_carga (id, categoria, codigo, descricao, atributos_json, ativo, criado_em, atualizado_em) VALUES
('11111111-1111-1111-1111-111111111101', 'TIPO_EMBALAGEM', 'BUNDLE', 'Feixe ou amarrado', NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('11111111-1111-1111-1111-111111111102', 'TIPO_EMBALAGEM', 'CRATE', 'Caixote', NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('11111111-1111-1111-1111-111111111103', 'TIPO_EMBALAGEM', 'PALLET', 'Palete', NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('11111111-1111-1111-1111-111111111104', 'TIPO_EMBALAGEM', 'PIECE', 'Peça individual', NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('11111111-1111-1111-1111-111111111105', 'CODIGO_ARMAZENAGEM', 'DRY_COVERED', 'Armazenagem coberta e seca', NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('11111111-1111-1111-1111-111111111106', 'CODIGO_ARMAZENAGEM', 'OPEN_YARD', 'Armazenagem em área descoberta', NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('11111111-1111-1111-1111-111111111107', 'CODIGO_MANUSEIO', 'FORKLIFT', 'Movimentação por empilhadeira', NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('11111111-1111-1111-1111-111111111108', 'CODIGO_MANUSEIO', 'MOBILE_CRANE', 'Movimentação por guindaste móvel', NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('11111111-1111-1111-1111-111111111109', 'TIPO_AVARIA', 'DENT', 'Amassamento', NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
('11111111-1111-1111-1111-111111111110', 'TIPO_AVARIA', 'WET', 'Carga molhada', NULL, TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
