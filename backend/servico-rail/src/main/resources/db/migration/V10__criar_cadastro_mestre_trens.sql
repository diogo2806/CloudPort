CREATE TABLE trem_cadastro (
    id BIGSERIAL PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    identificador VARCHAR(40) NOT NULL,
    operadora_ferroviaria VARCHAR(80) NOT NULL,
    descricao VARCHAR(120),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    observacoes VARCHAR(500),
    criado_por VARCHAR(120),
    alterado_por VARCHAR(120),
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_trem_cadastro_operadora_identificador UNIQUE (operadora_ferroviaria, identificador)
);

CREATE TABLE trem_cadastro_vagao (
    trem_cadastro_id BIGINT NOT NULL,
    ordem_vagao INTEGER NOT NULL,
    posicao_no_trem INTEGER NOT NULL,
    identificador_vagao VARCHAR(35) NOT NULL,
    tipo_vagao VARCHAR(40),
    CONSTRAINT fk_trem_cadastro_vagao_trem FOREIGN KEY (trem_cadastro_id) REFERENCES trem_cadastro(id),
    CONSTRAINT uk_trem_cadastro_vagao_identificador UNIQUE (trem_cadastro_id, identificador_vagao),
    CONSTRAINT uk_trem_cadastro_vagao_ordem UNIQUE (trem_cadastro_id, ordem_vagao)
);

CREATE INDEX idx_trem_cadastro_ativo ON trem_cadastro(ativo);
