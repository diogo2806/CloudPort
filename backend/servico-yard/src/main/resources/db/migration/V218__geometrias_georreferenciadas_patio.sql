CREATE TABLE IF NOT EXISTS geometria_patio (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(80) NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    bloco VARCHAR(40),
    linha INTEGER,
    coluna INTEGER,
    geojson TEXT NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    criado_por VARCHAR(120) NOT NULL,
    atualizado_por VARCHAR(120) NOT NULL,
    motivo_atualizacao VARCHAR(500) NOT NULL,
    CONSTRAINT uk_geometria_patio_codigo UNIQUE (codigo),
    CONSTRAINT ck_geometria_patio_tipo CHECK (tipo IN (
        'PILHA',
        'BLOCO',
        'VIA',
        'AREA_BLOQUEADA',
        'AREA_INTERDITADA',
        'EQUIPAMENTO'
    ))
);

CREATE INDEX IF NOT EXISTS idx_geometria_patio_tipo_ativa
    ON geometria_patio (tipo, ativa);

CREATE INDEX IF NOT EXISTS idx_geometria_patio_posicao
    ON geometria_patio (bloco, linha, coluna);
