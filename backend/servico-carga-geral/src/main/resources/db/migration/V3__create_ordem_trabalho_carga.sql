CREATE TABLE ordem_trabalho_carga (
    id UUID PRIMARY KEY,
    numero VARCHAR(80) NOT NULL UNIQUE,
    tipo VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL,
    prioridade INTEGER NOT NULL,
    janela_inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    janela_fim TIMESTAMP WITH TIME ZONE NOT NULL,
    local VARCHAR(120) NOT NULL,
    equipe_id VARCHAR(80),
    equipamento_id VARCHAR(80),
    motivo_cancelamento VARCHAR(1000),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    liberado_em TIMESTAMP WITH TIME ZONE,
    iniciado_em TIMESTAMP WITH TIME ZONE,
    concluido_em TIMESTAMP WITH TIME ZONE,
    cancelado_em TIMESTAMP WITH TIME ZONE,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_ordem_trabalho_janela CHECK (janela_fim > janela_inicio),
    CONSTRAINT ck_ordem_trabalho_prioridade CHECK (prioridade BETWEEN 1 AND 999)
);

CREATE TABLE item_ordem_trabalho_carga (
    id UUID PRIMARY KEY,
    ordem_id UUID NOT NULL REFERENCES ordem_trabalho_carga(id) ON DELETE CASCADE,
    lote_id UUID NOT NULL REFERENCES lote_carga(id),
    quantidade NUMERIC(19,3) NOT NULL,
    observacao VARCHAR(500),
    CONSTRAINT ck_item_ordem_quantidade CHECK (quantidade > 0),
    CONSTRAINT uk_item_ordem_lote UNIQUE (ordem_id, lote_id)
);

CREATE TABLE evento_ordem_trabalho_carga (
    id UUID PRIMARY KEY,
    ordem_id UUID NOT NULL REFERENCES ordem_trabalho_carga(id) ON DELETE CASCADE,
    tipo VARCHAR(40) NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    usuario VARCHAR(120) NOT NULL,
    ocorrido_em TIMESTAMP WITH TIME ZONE NOT NULL
);

CREATE INDEX idx_ordem_trabalho_status_prioridade ON ordem_trabalho_carga(status, prioridade, janela_inicio);
CREATE INDEX idx_item_ordem_lote ON item_ordem_trabalho_carga(lote_id);
CREATE INDEX idx_evento_ordem_ocorrido ON evento_ordem_trabalho_carga(ordem_id, ocorrido_em);
