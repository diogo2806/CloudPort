-- V105: Evidências e snapshot da validação completa de estiva de bobinas

ALTER TABLE plano_estiva_bulk
    ADD COLUMN IF NOT EXISTS versao_validacao_seguranca BIGINT,
    ADD COLUMN IF NOT EXISTS versao_especificacao_seguranca VARCHAR(60),
    ADD COLUMN IF NOT EXISTS referencia_regra_seguranca VARCHAR(120),
    ADD COLUMN IF NOT EXISTS validado_por_seguranca VARCHAR(100),
    ADD COLUMN IF NOT EXISTS validado_em_seguranca TIMESTAMP,
    ADD COLUMN IF NOT EXISTS resultado_validacao_seguranca VARCHAR(20) NOT NULL DEFAULT 'PENDENTE';

ALTER TABLE posicao_bobina
    ALTER COLUMN angulo_inclinacao DROP DEFAULT,
    ALTER COLUMN espessura_dunnage_mm DROP DEFAULT,
    ALTER COLUMN tipo_lashing DROP DEFAULT;

ALTER TABLE posicao_bobina
    ADD COLUMN IF NOT EXISTS quantidade_linhas_dunnage INTEGER,
    ADD COLUMN IF NOT EXISTS largura_dunnage_mm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS comprimento_contato_dunnage_mm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS quantidade_calcos INTEGER,
    ADD COLUMN IF NOT EXISTS espacamento_fileiras_mm DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS forca_requerida_lashing_kn DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS capacidade_lashing_disponivel_kn DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS sequencia_descarga INTEGER,
    ADD COLUMN IF NOT EXISTS referencia_regra VARCHAR(120),
    ADD COLUMN IF NOT EXISTS versao_especificacao VARCHAR(60),
    ADD COLUMN IF NOT EXISTS responsavel_validacao VARCHAR(100),
    ADD COLUMN IF NOT EXISTS resultado_validacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    ADD COLUMN IF NOT EXISTS validado_em TIMESTAMP;

ALTER TABLE material_lashing_bulk
    ADD COLUMN IF NOT EXISTS posicao_bobina_id BIGINT,
    ADD COLUMN IF NOT EXISTS ponto_amarracao VARCHAR(80),
    ADD COLUMN IF NOT EXISTS capacidade_nominal_kn DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS carga_trabalho_segura_kn DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS certificado VARCHAR(100),
    ADD COLUMN IF NOT EXISTS referencia_regra VARCHAR(120),
    ADD COLUMN IF NOT EXISTS versao_especificacao VARCHAR(60),
    ADD COLUMN IF NOT EXISTS responsavel_validacao VARCHAR(100),
    ADD COLUMN IF NOT EXISTS resultado_validacao VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    ADD COLUMN IF NOT EXISTS validado_em TIMESTAMP;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
          FROM pg_constraint
         WHERE conname = 'fk_material_lashing_posicao'
    ) THEN
        ALTER TABLE material_lashing_bulk
            ADD CONSTRAINT fk_material_lashing_posicao
            FOREIGN KEY (posicao_bobina_id)
            REFERENCES posicao_bobina(id)
            ON DELETE CASCADE;
    END IF;
END
$$;

CREATE INDEX IF NOT EXISTS idx_lashing_posicao
    ON material_lashing_bulk (posicao_bobina_id);

CREATE UNIQUE INDEX IF NOT EXISTS uk_posicao_bobina_sequencia_descarga
    ON posicao_bobina (plano_estiva_id, sequencia_descarga)
    WHERE sequencia_descarga IS NOT NULL;
