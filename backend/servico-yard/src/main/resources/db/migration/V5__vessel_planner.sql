-- V5: Tabelas para o Vessel Planner (Planner de Navio)

CREATE TABLE IF NOT EXISTS estivagem_plan (
    id                  BIGSERIAL PRIMARY KEY,
    bay_plan_id         BIGINT,
    codigo_navio        VARCHAR(50)  NOT NULL,
    codigo_viagem       VARCHAR(30)  NOT NULL,
    status              VARCHAR(20)  NOT NULL DEFAULT 'RASCUNHO',
    comprimento_lpp     DOUBLE PRECISION     DEFAULT 300.0,
    boca                DOUBLE PRECISION     DEFAULT 45.0,
    calado              DOUBLE PRECISION     DEFAULT 14.0,
    deslocamento        DOUBLE PRECISION     DEFAULT 90000.0,
    gm                  DOUBLE PRECISION     DEFAULT 1.5,
    tpc                 DOUBLE PRECISION     DEFAULT 75.0,
    lcb                 DOUBLE PRECISION     DEFAULT 150.0,
    trim_calculado      DOUBLE PRECISION,
    list_calculado      DOUBLE PRECISION,
    lcg_calculado       DOUBLE PRECISION,
    tcg_calculado       DOUBLE PRECISION,
    versao              BIGINT               NOT NULL DEFAULT 0,
    criado_em           TIMESTAMP            NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP            NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_estivagem_plan_navio_viagem
    ON estivagem_plan (codigo_navio, codigo_viagem);

CREATE INDEX IF NOT EXISTS idx_estivagem_plan_status
    ON estivagem_plan (status);

CREATE INDEX IF NOT EXISTS idx_estivagem_plan_bay_plan
    ON estivagem_plan (bay_plan_id);

CREATE TABLE IF NOT EXISTS slot_navio (
    id                  BIGSERIAL PRIMARY KEY,
    estivagem_plan_id   BIGINT       NOT NULL REFERENCES estivagem_plan(id) ON DELETE CASCADE,
    bay                 INTEGER      NOT NULL,
    row_bay             INTEGER      NOT NULL,
    tier                INTEGER      NOT NULL,
    tipo_slot           VARCHAR(20)  NOT NULL DEFAULT 'NORMAL',
    max_peso_kg         DOUBLE PRECISION,
    codigo_container    VARCHAR(20),
    iso_code            VARCHAR(10),
    peso_kg             DOUBLE PRECISION,
    porto_carga         VARCHAR(10),
    porto_descarga      VARCHAR(10),
    classe_imo          VARCHAR(10),
    reefer              BOOLEAN              DEFAULT FALSE,
    status_alertas      VARCHAR(20)          DEFAULT 'OK',
    criado_em           TIMESTAMP    NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_slot_navio_estivagem
    ON slot_navio (estivagem_plan_id);

CREATE INDEX IF NOT EXISTS idx_slot_navio_container
    ON slot_navio (codigo_container);

CREATE INDEX IF NOT EXISTS idx_slot_navio_posicao
    ON slot_navio (estivagem_plan_id, bay, row_bay, tier);
