CREATE TABLE IF NOT EXISTS visita_navio (
    id BIGSERIAL PRIMARY KEY,
    navio_id BIGINT NOT NULL REFERENCES navio_siderurgico (id),
    codigo_visita VARCHAR(60) NOT NULL UNIQUE,
    viagem_entrada VARCHAR(40),
    viagem_saida VARCHAR(40),
    linha_operadora VARCHAR(80),
    terminal_facility VARCHAR(80),
    berco_previsto VARCHAR(40),
    berco_atual VARCHAR(40),
    eta TIMESTAMP WITHOUT TIME ZONE,
    ata TIMESTAMP WITHOUT TIME ZONE,
    etb TIMESTAMP WITHOUT TIME ZONE,
    atb TIMESTAMP WITHOUT TIME ZONE,
    inicio_operacao TIMESTAMP WITHOUT TIME ZONE,
    fim_operacao TIMESTAMP WITHOUT TIME ZONE,
    etd TIMESTAMP WITHOUT TIME ZONE,
    atd TIMESTAMP WITHOUT TIME ZONE,
    janela_recebimento_inicio TIMESTAMP WITHOUT TIME ZONE,
    janela_recebimento_fim TIMESTAMP WITHOUT TIME ZONE,
    cutoff_operacional TIMESTAMP WITHOUT TIME ZONE,
    fase VARCHAR(30) NOT NULL,
    observacoes VARCHAR(1000),
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT ck_visita_navio_fase CHECK (fase IN ('PREVISTA', 'FUNDEADA', 'ATRACADA', 'OPERANDO', 'OPERACAO_CONCLUIDA', 'PARTIU', 'CANCELADA'))
);

CREATE TABLE IF NOT EXISTS item_operacao_navio (
    id BIGSERIAL PRIMARY KEY,
    visita_navio_id BIGINT NOT NULL REFERENCES visita_navio (id),
    tipo_movimento VARCHAR(20) NOT NULL,
    codigo_lote VARCHAR(80) NOT NULL,
    produto VARCHAR(120) NOT NULL,
    tipo_carga VARCHAR(40) NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    peso_unitario_toneladas NUMERIC(12, 3),
    peso_total_toneladas NUMERIC(14, 3) NOT NULL CHECK (peso_total_toneladas > 0),
    porao_planejado INTEGER,
    porao_real INTEGER,
    posicao_planejada VARCHAR(80),
    posicao_real VARCHAR(80),
    origem_patio VARCHAR(80),
    destino_patio VARCHAR(80),
    sequencia_operacional INTEGER,
    status VARCHAR(30) NOT NULL,
    motivo_bloqueio VARCHAR(500),
    observacoes VARCHAR(1000),
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_item_operacao_navio_lote_mov UNIQUE (visita_navio_id, tipo_movimento, codigo_lote),
    CONSTRAINT ck_item_operacao_navio_mov CHECK (tipo_movimento IN ('EMBARQUE', 'DESCARGA', 'RESTOW')),
    CONSTRAINT ck_item_operacao_navio_status CHECK (status IN ('PLANEJADO', 'LIBERADO', 'EM_MOVIMENTO', 'OPERADO', 'BLOQUEADO', 'CANCELADO'))
);

CREATE TABLE IF NOT EXISTS plano_estiva_navio (
    id BIGSERIAL PRIMARY KEY,
    visita_navio_id BIGINT NOT NULL REFERENCES visita_navio (id),
    versao INTEGER NOT NULL CHECK (versao > 0),
    status VARCHAR(30) NOT NULL,
    peso_total_planejado NUMERIC(14, 3) NOT NULL DEFAULT 0,
    peso_total_realizado NUMERIC(14, 3) NOT NULL DEFAULT 0,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    validado_em TIMESTAMP WITHOUT TIME ZONE,
    CONSTRAINT uk_plano_estiva_navio_versao UNIQUE (visita_navio_id, versao),
    CONSTRAINT ck_plano_estiva_navio_status CHECK (status IN ('RASCUNHO', 'VALIDADO', 'EM_EXECUCAO', 'CONCLUIDO', 'CANCELADO'))
);

CREATE TABLE IF NOT EXISTS posicao_estiva_navio (
    id BIGSERIAL PRIMARY KEY,
    plano_estiva_id BIGINT NOT NULL REFERENCES plano_estiva_navio (id) ON DELETE CASCADE,
    item_operacao_id BIGINT NOT NULL REFERENCES item_operacao_navio (id),
    porao INTEGER NOT NULL CHECK (porao > 0),
    camada INTEGER NOT NULL CHECK (camada > 0),
    coluna INTEGER NOT NULL CHECK (coluna > 0),
    bordo VARCHAR(20) NOT NULL,
    sequencia INTEGER NOT NULL CHECK (sequencia > 0),
    peso_toneladas NUMERIC(14, 3) NOT NULL CHECK (peso_toneladas > 0),
    status VARCHAR(30) NOT NULL,
    CONSTRAINT ck_posicao_estiva_navio_bordo CHECK (bordo IN ('BB', 'BE', 'CENTRO'))
);

CREATE TABLE IF NOT EXISTS evento_visita_navio (
    id BIGSERIAL PRIMARY KEY,
    visita_navio_id BIGINT NOT NULL REFERENCES visita_navio (id) ON DELETE CASCADE,
    item_operacao_id BIGINT REFERENCES item_operacao_navio (id) ON DELETE SET NULL,
    tipo_evento VARCHAR(80) NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    usuario VARCHAR(120) NOT NULL,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    dados_antes VARCHAR(2000),
    dados_depois VARCHAR(2000)
);

CREATE UNIQUE INDEX IF NOT EXISTS uk_posicao_estiva_navio_ativa
    ON posicao_estiva_navio (plano_estiva_id, porao, camada, coluna, bordo)
    WHERE status <> 'CANCELADO';

CREATE INDEX IF NOT EXISTS idx_visita_navio_navio ON visita_navio (navio_id);
CREATE INDEX IF NOT EXISTS idx_visita_navio_fase_eta ON visita_navio (fase, eta);
CREATE INDEX IF NOT EXISTS idx_item_operacao_navio_visita ON item_operacao_navio (visita_navio_id);
CREATE INDEX IF NOT EXISTS idx_item_operacao_navio_status ON item_operacao_navio (status);
CREATE INDEX IF NOT EXISTS idx_plano_estiva_navio_visita ON plano_estiva_navio (visita_navio_id);
CREATE INDEX IF NOT EXISTS idx_posicao_estiva_navio_plano ON posicao_estiva_navio (plano_estiva_id);
CREATE INDEX IF NOT EXISTS idx_evento_visita_navio_visita ON evento_visita_navio (visita_navio_id, criado_em DESC);
