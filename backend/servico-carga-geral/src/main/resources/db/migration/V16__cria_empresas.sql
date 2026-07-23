CREATE TABLE IF NOT EXISTS empresa (
    id UUID PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL,
    razao_social VARCHAR(180) NOT NULL,
    nome_fantasia VARCHAR(180),
    documento VARCHAR(40) NOT NULL,
    documento_normalizado VARCHAR(40) NOT NULL,
    inscricao_estadual VARCHAR(40),
    endereco VARCHAR(500),
    contato VARCHAR(180),
    email VARCHAR(180),
    telefone VARCHAR(40),
    pais VARCHAR(80) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    observacoes VARCHAR(1000),
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_empresa_codigo UNIQUE (codigo),
    CONSTRAINT uk_empresa_documento UNIQUE (documento_normalizado)
);

CREATE TABLE IF NOT EXISTS empresa_papel (
    empresa_id UUID NOT NULL,
    papel VARCHAR(30) NOT NULL,
    CONSTRAINT fk_empresa_papel_empresa FOREIGN KEY (empresa_id) REFERENCES empresa (id) ON DELETE CASCADE,
    CONSTRAINT uk_empresa_papel UNIQUE (empresa_id, papel)
);

CREATE INDEX IF NOT EXISTS idx_empresa_razao_social ON empresa (razao_social);
CREATE INDEX IF NOT EXISTS idx_empresa_ativo ON empresa (ativo);
CREATE INDEX IF NOT EXISTS idx_empresa_papel_papel ON empresa_papel (papel);
