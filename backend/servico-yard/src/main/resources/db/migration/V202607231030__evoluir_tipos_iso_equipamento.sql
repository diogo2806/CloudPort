ALTER TABLE tipo_equipamento_inventario
    ALTER COLUMN codigo_iso TYPE VARCHAR(4),
    ADD COLUMN IF NOT EXISTS grupo_iso_id BIGINT,
    ADD COLUMN IF NOT EXISTS arquetipo_iso VARCHAR(30),
    ADD COLUMN IF NOT EXISTS indicador_arquetipo BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS provisorio_edi BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN IF NOT EXISTS criado_por VARCHAR(100),
    ADD COLUMN IF NOT EXISTS atualizado_por VARCHAR(100);

CREATE UNIQUE INDEX IF NOT EXISTS uk_tipo_equipamento_codigo_iso
    ON tipo_equipamento_inventario (UPPER(codigo_iso))
    WHERE codigo_iso IS NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint constraint_definition
        JOIN pg_class relation ON relation.oid = constraint_definition.conrelid
        WHERE constraint_definition.conname = 'fk_tipo_equipamento_grupo_iso'
          AND relation.relname = 'tipo_equipamento_inventario'
    ) THEN
        ALTER TABLE tipo_equipamento_inventario
            ADD CONSTRAINT fk_tipo_equipamento_grupo_iso
            FOREIGN KEY (grupo_iso_id) REFERENCES grupo_iso_equipamento(id);
    END IF;
END $$;

UPDATE tipo_equipamento_inventario
SET codigo_iso = UPPER(TRIM(codigo_iso)),
    criado_por = COALESCE(criado_por, 'MIGRACAO'),
    atualizado_por = COALESCE(atualizado_por, 'MIGRACAO')
WHERE codigo_iso IS NOT NULL;

UPDATE tipo_equipamento_inventario
SET arquetipo_iso = codigo_iso,
    indicador_arquetipo = TRUE
WHERE codigo_iso IN ('22G1', '42G1', '45G1', '42R1');
