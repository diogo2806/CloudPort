CREATE TABLE comando_operacao_grafica_2d (
    id BIGSERIAL PRIMARY KEY,
    command_id VARCHAR(120) NOT NULL,
    tipo VARCHAR(80) NOT NULL,
    status VARCHAR(30) NOT NULL,
    motivo VARCHAR(1000),
    payload_json TEXT NOT NULL,
    solicitado_por VARCHAR(120) NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_comando_operacao_grafica_2d_command_id UNIQUE (command_id)
);

CREATE INDEX idx_comando_operacao_grafica_2d_tipo_status
    ON comando_operacao_grafica_2d (tipo, status, criado_em DESC);

CREATE TABLE workspace_grafico_2d (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(160) NOT NULL,
    escopo VARCHAR(20) NOT NULL,
    papel VARCHAR(80),
    proprietario VARCHAR(120) NOT NULL,
    versao BIGINT NOT NULL,
    conteudo_json TEXT NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_workspace_grafico_2d_escopo
        CHECK (escopo IN ('INDIVIDUAL', 'EQUIPE', 'PAPEL', 'PADRAO')),
    CONSTRAINT ck_workspace_grafico_2d_papel
        CHECK (escopo <> 'PAPEL' OR papel IS NOT NULL),
    CONSTRAINT uk_workspace_grafico_2d_versao
        UNIQUE (nome, escopo, proprietario, versao)
);

CREATE INDEX idx_workspace_grafico_2d_visibilidade
    ON workspace_grafico_2d (escopo, papel, proprietario, criado_em DESC);
