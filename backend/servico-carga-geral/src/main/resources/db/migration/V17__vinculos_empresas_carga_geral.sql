CREATE TABLE IF NOT EXISTS vinculo_empresa_carga (
    id UUID PRIMARY KEY,
    tipo_recurso VARCHAR(30) NOT NULL,
    recurso_id UUID NOT NULL,
    papel VARCHAR(30) NOT NULL,
    empresa_id UUID NOT NULL,
    empresa_nome_snapshot VARCHAR(180) NOT NULL,
    vinculado_por VARCHAR(180) NOT NULL,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_vinculo_empresa_carga_empresa
        FOREIGN KEY (empresa_id) REFERENCES empresa(id),
    CONSTRAINT ck_vinculo_empresa_carga_tipo
        CHECK (tipo_recurso IN ('CONHECIMENTO', 'LOTE')),
    CONSTRAINT uk_vinculo_empresa_carga_recurso_papel
        UNIQUE (tipo_recurso, recurso_id, papel)
);

CREATE INDEX IF NOT EXISTS idx_vinculo_empresa_carga_recurso
    ON vinculo_empresa_carga (tipo_recurso, recurso_id);

CREATE INDEX IF NOT EXISTS idx_vinculo_empresa_carga_empresa
    ON vinculo_empresa_carga (empresa_id);

CREATE TABLE IF NOT EXISTS auditoria_vinculo_empresa_carga (
    id UUID PRIMARY KEY,
    tipo_recurso VARCHAR(30) NOT NULL,
    recurso_id UUID NOT NULL,
    papel VARCHAR(30) NOT NULL,
    empresa_anterior_id UUID,
    empresa_nova_id UUID,
    acao VARCHAR(30) NOT NULL,
    usuario VARCHAR(180) NOT NULL,
    ocorrido_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT ck_auditoria_vinculo_empresa_carga_tipo
        CHECK (tipo_recurso IN ('CONHECIMENTO', 'LOTE')),
    CONSTRAINT ck_auditoria_vinculo_empresa_carga_acao
        CHECK (acao IN ('INCLUSAO', 'ALTERACAO', 'REMOCAO'))
);

CREATE INDEX IF NOT EXISTS idx_auditoria_vinculo_empresa_carga_recurso
    ON auditoria_vinculo_empresa_carga (tipo_recurso, recurso_id, ocorrido_em DESC);
