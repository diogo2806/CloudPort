-- V106: geometria real e versionada do navio para o Vessel Planner

CREATE TABLE perfil_geometria_navio (
    id BIGSERIAL PRIMARY KEY,
    codigo_navio VARCHAR(50) NOT NULL,
    versao_perfil BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    condicao_carregamento VARCHAR(80) NOT NULL,
    comprimento_lpp DOUBLE PRECISION NOT NULL,
    boca DOUBLE PRECISION NOT NULL,
    calado DOUBLE PRECISION NOT NULL,
    deslocamento DOUBLE PRECISION NOT NULL,
    gm DOUBLE PRECISION NOT NULL,
    tpc DOUBLE PRECISION NOT NULL,
    lcb DOUBLE PRECISION NOT NULL,
    versao_registro BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_perfil_geometria_navio_codigo_versao
        UNIQUE (codigo_navio, versao_perfil),
    CONSTRAINT ck_perfil_geometria_navio_status
        CHECK (status IN ('RASCUNHO', 'APROVADO', 'INATIVO')),
    CONSTRAINT ck_perfil_geometria_navio_versao
        CHECK (versao_perfil > 0),
    CONSTRAINT ck_perfil_geometria_navio_dimensoes
        CHECK (comprimento_lpp > 0 AND boca > 0 AND calado > 0
            AND deslocamento > 0 AND gm > 0 AND tpc > 0
            AND lcb >= 0 AND lcb <= comprimento_lpp)
);

CREATE TABLE slot_perfil_navio (
    id BIGSERIAL PRIMARY KEY,
    perfil_geometria_navio_id BIGINT NOT NULL,
    bay INTEGER NOT NULL,
    row_bay INTEGER NOT NULL,
    tier INTEGER NOT NULL,
    tipo_slot VARCHAR(30) NOT NULL,
    codigo_hatch_cover VARCHAR(40),
    sobre_hatch_cover BOOLEAN NOT NULL DEFAULT FALSE,
    restrito BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_restricao VARCHAR(255),
    tomada_reefer BOOLEAN NOT NULL DEFAULT FALSE,
    aceita_20_pes BOOLEAN NOT NULL DEFAULT FALSE,
    aceita_40_pes BOOLEAN NOT NULL DEFAULT FALSE,
    aceita_45_pes BOOLEAN NOT NULL DEFAULT FALSE,
    max_peso_kg DOUBLE PRECISION NOT NULL,
    max_peso_pilha_kg DOUBLE PRECISION NOT NULL,
    CONSTRAINT fk_slot_perfil_navio_perfil
        FOREIGN KEY (perfil_geometria_navio_id) REFERENCES perfil_geometria_navio(id),
    CONSTRAINT uk_slot_perfil_navio_posicao
        UNIQUE (perfil_geometria_navio_id, bay, row_bay, tier),
    CONSTRAINT ck_slot_perfil_navio_coordenadas
        CHECK (bay > 0 AND row_bay > 0 AND tier > 0),
    CONSTRAINT ck_slot_perfil_navio_tipo
        CHECK (tipo_slot IN ('NORMAL', 'REEFER', 'PERIGOSO', 'REEFER_PERIGOSO',
            'OOG', 'ESCOTILHA', 'RESTRITO')),
    CONSTRAINT ck_slot_perfil_navio_pesos
        CHECK (max_peso_kg > 0 AND max_peso_pilha_kg >= max_peso_kg)
);

CREATE INDEX idx_perfil_geometria_navio_aprovado
    ON perfil_geometria_navio (codigo_navio, status, versao_perfil DESC);

CREATE INDEX idx_slot_perfil_navio_perfil
    ON slot_perfil_navio (perfil_geometria_navio_id);

ALTER TABLE estivagem_plan
    ADD COLUMN IF NOT EXISTS perfil_geometria_id BIGINT,
    ADD COLUMN IF NOT EXISTS perfil_geometria_versao BIGINT,
    ADD COLUMN IF NOT EXISTS condicao_carregamento VARCHAR(80);

ALTER TABLE estivagem_plan
    DROP CONSTRAINT IF EXISTS fk_estivagem_plan_perfil_geometria;

ALTER TABLE estivagem_plan
    ADD CONSTRAINT fk_estivagem_plan_perfil_geometria
        FOREIGN KEY (perfil_geometria_id) REFERENCES perfil_geometria_navio(id);

ALTER TABLE estivagem_plan
    ALTER COLUMN comprimento_lpp DROP DEFAULT,
    ALTER COLUMN boca DROP DEFAULT,
    ALTER COLUMN calado DROP DEFAULT,
    ALTER COLUMN deslocamento DROP DEFAULT,
    ALTER COLUMN gm DROP DEFAULT,
    ALTER COLUMN tpc DROP DEFAULT,
    ALTER COLUMN lcb DROP DEFAULT;

ALTER TABLE slot_navio
    ALTER COLUMN tipo_slot TYPE VARCHAR(30),
    ADD COLUMN IF NOT EXISTS codigo_hatch_cover VARCHAR(40),
    ADD COLUMN IF NOT EXISTS sobre_hatch_cover BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS restrito BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS motivo_restricao VARCHAR(255),
    ADD COLUMN IF NOT EXISTS tomada_reefer BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS aceita_20_pes BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS aceita_40_pes BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS aceita_45_pes BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS max_peso_pilha_kg DOUBLE PRECISION;

CREATE INDEX IF NOT EXISTS idx_estivagem_plan_perfil_geometria
    ON estivagem_plan (perfil_geometria_id, perfil_geometria_versao);

CREATE INDEX IF NOT EXISTS idx_slot_navio_pilha
    ON slot_navio (estivagem_plan_id, bay, row_bay, tier);
