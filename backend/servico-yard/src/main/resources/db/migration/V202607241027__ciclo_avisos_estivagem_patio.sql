CREATE TABLE aviso_estivagem_patio (
    id BIGSERIAL PRIMARY KEY,
    chave_estavel VARCHAR(255) NOT NULL,
    unidade_id BIGINT NOT NULL,
    posicao_id BIGINT NOT NULL,
    codigo_unidade VARCHAR(40) NOT NULL,
    codigo_posicao VARCHAR(120) NOT NULL,
    bloco VARCHAR(40),
    linha INTEGER NOT NULL,
    coluna INTEGER NOT NULL,
    camada VARCHAR(40) NOT NULL,
    regra VARCHAR(40) NOT NULL,
    severidade VARCHAR(20) NOT NULL,
    estado VARCHAR(40) NOT NULL,
    valor_observado VARCHAR(1000) NOT NULL,
    valor_esperado VARCHAR(1000),
    acao_sugerida VARCHAR(1000),
    responsavel VARCHAR(120),
    prazo TIMESTAMP,
    acao_corretiva VARCHAR(2000),
    evidencia VARCHAR(2000),
    resultado_revalidacao VARCHAR(2000),
    ocorrencias INTEGER NOT NULL DEFAULT 1,
    bloqueia_operacao BOOLEAN NOT NULL DEFAULT FALSE,
    aberto_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ultima_revalidacao_em TIMESTAMP,
    resolvido_em TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_aviso_estivagem_unidade
        FOREIGN KEY (unidade_id) REFERENCES conteiner_patio (id),
    CONSTRAINT fk_aviso_estivagem_posicao
        FOREIGN KEY (posicao_id) REFERENCES posicao_patio (id),
    CONSTRAINT uk_aviso_estivagem_chave UNIQUE (chave_estavel),
    CONSTRAINT uk_aviso_estivagem_unidade_posicao_regra
        UNIQUE (codigo_unidade, codigo_posicao, regra)
);

CREATE INDEX idx_aviso_estivagem_estado_severidade
    ON aviso_estivagem_patio (estado, severidade, atualizado_em DESC);

CREATE INDEX idx_aviso_estivagem_unidade
    ON aviso_estivagem_patio (codigo_unidade, estado);

CREATE INDEX idx_aviso_estivagem_posicao
    ON aviso_estivagem_patio (codigo_posicao, estado);

CREATE INDEX idx_aviso_estivagem_responsavel_prazo
    ON aviso_estivagem_patio (responsavel, prazo)
    WHERE estado <> 'RESOLVIDO';

CREATE TABLE historico_aviso_estivagem_patio (
    id BIGSERIAL PRIMARY KEY,
    aviso_id BIGINT NOT NULL,
    evento VARCHAR(40) NOT NULL,
    estado_anterior VARCHAR(40),
    estado_novo VARCHAR(40) NOT NULL,
    ator VARCHAR(120) NOT NULL,
    detalhes VARCHAR(2000),
    evidencia VARCHAR(2000),
    resultado VARCHAR(2000),
    ocorrido_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_historico_aviso_estivagem
        FOREIGN KEY (aviso_id) REFERENCES aviso_estivagem_patio (id) ON DELETE CASCADE
);

CREATE INDEX idx_historico_aviso_estivagem
    ON historico_aviso_estivagem_patio (aviso_id, ocorrido_em ASC);
