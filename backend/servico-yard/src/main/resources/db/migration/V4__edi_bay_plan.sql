-- V4: Tabelas para integração EDI (BAPLIE/COPRAR/COARRI)

CREATE TABLE IF NOT EXISTS bay_plan (
    id              BIGSERIAL PRIMARY KEY,
    codigo_navio    VARCHAR(50)  NOT NULL,
    nome_navio      VARCHAR(100),
    codigo_viagem   VARCHAR(30)  NOT NULL,
    porto_carga     VARCHAR(10),
    porto_descarga  VARCHAR(10),
    status          VARCHAR(20)  NOT NULL DEFAULT 'RASCUNHO',
    origem_mensagem VARCHAR(20),
    criado_em       TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em   TIMESTAMP    NOT NULL DEFAULT NOW(),
    versao          BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_bay_plan_navio_viagem
    ON bay_plan (codigo_navio, codigo_viagem);

CREATE INDEX IF NOT EXISTS idx_bay_plan_status
    ON bay_plan (status);

CREATE TABLE IF NOT EXISTS bay_plan_container (
    id               BIGSERIAL PRIMARY KEY,
    bay_plan_id      BIGINT       NOT NULL REFERENCES bay_plan(id) ON DELETE CASCADE,
    codigo_container VARCHAR(20)  NOT NULL,
    iso_code         VARCHAR(10),
    bay              INTEGER,
    row_bay          INTEGER,
    tier             INTEGER,
    porto_carga      VARCHAR(10),
    porto_descarga   VARCHAR(10),
    peso_kg          DOUBLE PRECISION,
    referencia_bl    VARCHAR(30),
    tipo_operacao    VARCHAR(20)  NOT NULL,
    status_operacao  VARCHAR(30),
    horario_operacao TIMESTAMP,
    linha_yard       INTEGER,
    coluna_yard      INTEGER,
    criado_em        TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em    TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_bay_plan_container_bay_plan
    ON bay_plan_container (bay_plan_id);

CREATE INDEX IF NOT EXISTS idx_bay_plan_container_codigo
    ON bay_plan_container (codigo_container);
