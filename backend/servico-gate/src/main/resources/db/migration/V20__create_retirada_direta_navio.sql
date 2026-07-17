CREATE TABLE retirada_direta_navio (
    id BIGSERIAL PRIMARY KEY,
    codigo_autorizacao VARCHAR(80) NOT NULL,
    identificador_carga VARCHAR(80) NOT NULL,
    tipo_carga VARCHAR(60) NOT NULL,
    visita_navio VARCHAR(80) NOT NULL,
    cliente_nome VARCHAR(120) NOT NULL,
    cliente_documento VARCHAR(30) NOT NULL,
    status VARCHAR(40) NOT NULL,
    saida_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    operador VARCHAR(80) NOT NULL,
    observacao VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_retirada_direta_navio_autorizacao_ci
    ON retirada_direta_navio (UPPER(codigo_autorizacao));

CREATE UNIQUE INDEX uk_retirada_direta_navio_carga_ci
    ON retirada_direta_navio (UPPER(identificador_carga));

CREATE INDEX idx_retirada_direta_navio_saida_em
    ON retirada_direta_navio (saida_em DESC);

CREATE INDEX idx_retirada_direta_navio_visita
    ON retirada_direta_navio (visita_navio);
