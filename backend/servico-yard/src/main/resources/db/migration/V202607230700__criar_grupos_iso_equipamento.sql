CREATE TABLE IF NOT EXISTS grupo_iso_equipamento (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(10) NOT NULL,
    descricao VARCHAR(120) NOT NULL,
    categoria VARCHAR(30) NOT NULL,
    refrigerado BOOLEAN NOT NULL DEFAULT FALSE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_por VARCHAR(100) NOT NULL DEFAULT 'migration',
    atualizado_por VARCHAR(100) NOT NULL DEFAULT 'migration',
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_grupo_iso_equipamento_codigo UNIQUE (codigo)
);

INSERT INTO grupo_iso_equipamento (codigo, descricao, categoria, refrigerado)
VALUES
    ('DRY', 'CONTÊINER DE CARGA SECA', 'CONTEINER', FALSE),
    ('REEFER', 'CONTÊINER REFRIGERADO', 'CONTEINER', TRUE),
    ('OPEN', 'CONTÊINER OPEN TOP', 'CONTEINER', FALSE),
    ('FLAT', 'CONTÊINER FLAT RACK', 'CONTEINER', FALSE),
    ('TANK', 'CONTÊINER TANQUE', 'CONTEINER', FALSE),
    ('CHASSI', 'CHASSI PORTA-CONTÊINER', 'CHASSI', FALSE),
    ('CARRETA', 'CARRETA RODOVIÁRIA', 'CARRETA', FALSE),
    ('ACESS', 'ACESSÓRIO OPERACIONAL', 'ACESSORIO', FALSE)
ON CONFLICT (codigo) DO NOTHING;

ALTER TABLE tipo_equipamento_inventario
    ADD COLUMN IF NOT EXISTS grupo_iso_id BIGINT;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_constraint WHERE conname = 'fk_tipo_equipamento_grupo_iso'
    ) THEN
        ALTER TABLE tipo_equipamento_inventario
            ADD CONSTRAINT fk_tipo_equipamento_grupo_iso
            FOREIGN KEY (grupo_iso_id) REFERENCES grupo_iso_equipamento(id);
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_tipo_equipamento_grupo_iso
    ON tipo_equipamento_inventario (grupo_iso_id);
