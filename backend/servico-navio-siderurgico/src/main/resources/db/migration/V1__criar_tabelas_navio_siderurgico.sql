CREATE TABLE IF NOT EXISTS navio_siderurgico (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    codigo_imo VARCHAR(10) NOT NULL UNIQUE,
    pais_bandeira VARCHAR(60) NOT NULL,
    empresa_armadora VARCHAR(80) NOT NULL,
    tipo_navio VARCHAR(40) NOT NULL,
    loa_metros NUMERIC(8, 2),
    dwt_toneladas NUMERIC(12, 2),
    quantidade_poroes INTEGER NOT NULL CHECK (quantidade_poroes > 0),
    status VARCHAR(30) NOT NULL,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS operacao_siderurgica (
    id BIGSERIAL PRIMARY KEY,
    navio_id BIGINT NOT NULL REFERENCES navio_siderurgico (id),
    tipo_operacao VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    berco VARCHAR(30),
    viagem VARCHAR(40),
    eta TIMESTAMP WITHOUT TIME ZONE,
    inicio_operacao TIMESTAMP WITHOUT TIME ZONE,
    fim_operacao TIMESTAMP WITHOUT TIME ZONE,
    origem VARCHAR(80),
    destino VARCHAR(80),
    observacoes VARCHAR(500),
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT ck_operacao_tipo CHECK (tipo_operacao IN ('EMBARQUE', 'DESCARGA'))
);

CREATE TABLE IF NOT EXISTS item_carga_siderurgica (
    id BIGSERIAL PRIMARY KEY,
    operacao_id BIGINT NOT NULL REFERENCES operacao_siderurgica (id),
    codigo_lote VARCHAR(60) NOT NULL,
    tipo_carga VARCHAR(40) NOT NULL,
    produto VARCHAR(120) NOT NULL,
    quantidade INTEGER NOT NULL CHECK (quantidade > 0),
    peso_unitario_toneladas NUMERIC(10, 3),
    peso_total_toneladas NUMERIC(12, 3) NOT NULL,
    porao INTEGER,
    posicao_bordo VARCHAR(40),
    origem_patio VARCHAR(80),
    destino_patio VARCHAR(80),
    sequencia_operacional INTEGER,
    status VARCHAR(30) NOT NULL,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_item_carga_lote_operacao UNIQUE (operacao_id, codigo_lote)
);

CREATE INDEX IF NOT EXISTS idx_operacao_siderurgica_navio ON operacao_siderurgica (navio_id);
CREATE INDEX IF NOT EXISTS idx_operacao_siderurgica_tipo ON operacao_siderurgica (tipo_operacao);
CREATE INDEX IF NOT EXISTS idx_item_carga_siderurgica_operacao ON item_carga_siderurgica (operacao_id);
CREATE INDEX IF NOT EXISTS idx_item_carga_siderurgica_status ON item_carga_siderurgica (status);
