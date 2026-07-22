ALTER TABLE amarrado_carga
    ADD COLUMN destino_direcionamento VARCHAR(100),
    ADD COLUMN motivo_direcionamento VARCHAR(255),
    ADD COLUMN direcionado_em TIMESTAMP WITH TIME ZONE;

UPDATE amarrado_carga
SET destino_direcionamento = 'AREA_TRIAGEM',
    motivo_direcionamento = 'Direcionamento legado definido durante a migração.',
    direcionado_em = COALESCE(atualizado_em, criado_em, CURRENT_TIMESTAMP)
WHERE destino_direcionamento IS NULL;

ALTER TABLE amarrado_carga
    ALTER COLUMN destino_direcionamento SET NOT NULL,
    ALTER COLUMN motivo_direcionamento SET NOT NULL,
    ALTER COLUMN direcionado_em SET NOT NULL;

CREATE INDEX idx_amarrado_carga_destino
    ON amarrado_carga (destino_direcionamento);
