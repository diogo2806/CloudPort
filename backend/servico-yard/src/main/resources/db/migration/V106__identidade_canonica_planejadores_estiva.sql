ALTER TABLE navio_granel
    ADD COLUMN IF NOT EXISTS navio_cadastro_id BIGINT,
    ADD COLUMN IF NOT EXISTS versao_perfil BIGINT NOT NULL DEFAULT 1,
    ADD COLUMN IF NOT EXISTS versao_navio_canonico BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS uk_navio_granel_cadastro_versao
    ON navio_granel (navio_cadastro_id, versao_perfil)
    WHERE navio_cadastro_id IS NOT NULL;

ALTER TABLE plano_estiva_bulk
    ADD COLUMN IF NOT EXISTS navio_cadastro_id BIGINT,
    ADD COLUMN IF NOT EXISTS visita_navio_id BIGINT,
    ADD COLUMN IF NOT EXISTS codigo_visita VARCHAR(60),
    ADD COLUMN IF NOT EXISTS versao_perfil_navio BIGINT,
    ADD COLUMN IF NOT EXISTS versao_navio_canonico BIGINT,
    ADD COLUMN IF NOT EXISTS versao_visita BIGINT;

UPDATE plano_estiva_bulk plano
SET navio_cadastro_id = perfil.navio_cadastro_id,
    versao_perfil_navio = perfil.versao_perfil,
    versao_navio_canonico = perfil.versao_navio_canonico
FROM navio_granel perfil
WHERE plano.navio_granel_id = perfil.id
  AND plano.navio_cadastro_id IS NULL
  AND perfil.navio_cadastro_id IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_plano_estiva_bulk_navio_canonico ON plano_estiva_bulk (navio_cadastro_id);
CREATE INDEX IF NOT EXISTS idx_plano_estiva_bulk_visita ON plano_estiva_bulk (visita_navio_id);

ALTER TABLE estivagem_plan
    ADD COLUMN IF NOT EXISTS navio_cadastro_id BIGINT,
    ADD COLUMN IF NOT EXISTS visita_navio_id BIGINT,
    ADD COLUMN IF NOT EXISTS codigo_visita VARCHAR(60),
    ADD COLUMN IF NOT EXISTS versao_navio_canonico BIGINT,
    ADD COLUMN IF NOT EXISTS versao_visita BIGINT;

CREATE INDEX IF NOT EXISTS idx_estivagem_plan_navio_canonico ON estivagem_plan (navio_cadastro_id);
CREATE INDEX IF NOT EXISTS idx_estivagem_plan_visita ON estivagem_plan (visita_navio_id);
