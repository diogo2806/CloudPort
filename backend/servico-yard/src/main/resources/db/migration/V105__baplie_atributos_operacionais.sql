-- V105: atributos operacionais e de segurança estruturados do BAPLIE

ALTER TABLE bay_plan_container
    ADD COLUMN IF NOT EXISTS unidade_peso_original VARCHAR(10),
    ADD COLUMN IF NOT EXISTS peso_vgm_kg DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS unidade_vgm_original VARCHAR(10),
    ADD COLUMN IF NOT EXISTS origem_vgm VARCHAR(30),
    ADD COLUMN IF NOT EXISTS status_vgm VARCHAR(30),
    ADD COLUMN IF NOT EXISTS estado_carga VARCHAR(20) NOT NULL DEFAULT 'DESCONHECIDO',
    ADD COLUMN IF NOT EXISTS reefer BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS temperatura_requerida_c DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS temperatura_minima_c DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS temperatura_maxima_c DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS perigoso BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS classe_imo VARCHAR(20),
    ADD COLUMN IF NOT EXISTS numero_onu VARCHAR(20),
    ADD COLUMN IF NOT EXISTS grupo_embalagem VARCHAR(20),
    ADD COLUMN IF NOT EXISTS grupo_segregacao VARCHAR(50),
    ADD COLUMN IF NOT EXISTS codigo_emergencia VARCHAR(100),
    ADD COLUMN IF NOT EXISTS oog BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excesso_frontal_cm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS excesso_traseiro_cm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS excesso_esquerdo_cm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS excesso_direito_cm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS excesso_altura_cm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS instrucao_manuseio VARCHAR(500),
    ADD COLUMN IF NOT EXISTS segmentos_originais TEXT;

ALTER TABLE slot_navio
    ADD COLUMN IF NOT EXISTS peso_vgm_kg DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS estado_carga VARCHAR(20) NOT NULL DEFAULT 'DESCONHECIDO',
    ADD COLUMN IF NOT EXISTS perigoso BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS numero_onu VARCHAR(20),
    ADD COLUMN IF NOT EXISTS grupo_segregacao VARCHAR(50),
    ADD COLUMN IF NOT EXISTS temperatura_requerida_c DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS temperatura_minima_c DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS temperatura_maxima_c DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS oog BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS excesso_frontal_cm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS excesso_traseiro_cm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS excesso_esquerdo_cm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS excesso_direito_cm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS excesso_altura_cm DOUBLE PRECISION;

CREATE INDEX IF NOT EXISTS idx_bay_plan_container_perigoso
    ON bay_plan_container (bay_plan_id, perigoso);

CREATE INDEX IF NOT EXISTS idx_bay_plan_container_reefer
    ON bay_plan_container (bay_plan_id, reefer);

CREATE INDEX IF NOT EXISTS idx_bay_plan_container_oog
    ON bay_plan_container (bay_plan_id, oog);
