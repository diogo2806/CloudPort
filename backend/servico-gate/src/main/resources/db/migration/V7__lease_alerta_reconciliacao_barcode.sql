ALTER TABLE reconciliacao_barcode
    ADD COLUMN IF NOT EXISTS alerta_chave_idempotencia VARCHAR(160),
    ADD COLUMN IF NOT EXISTS alerta_reivindicacao_token VARCHAR(64),
    ADD COLUMN IF NOT EXISTS alerta_reivindicado_em TIMESTAMP,
    ADD COLUMN IF NOT EXISTS alerta_lease_ate TIMESTAMP;

UPDATE reconciliacao_barcode
SET alerta_chave_idempotencia = 'reconciliacao-barcode-' || id || '-webhook'
WHERE alerta_chave_idempotencia IS NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_reconciliacao_barcode_alerta_idempotencia
    ON reconciliacao_barcode (alerta_chave_idempotencia)
    WHERE alerta_chave_idempotencia IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_reconciliacao_barcode_alerta_reivindicacao
    ON reconciliacao_barcode (
        alerta_enviado,
        resolvido_em,
        status_entrega_alerta,
        alerta_lease_ate,
        detectado_em
    );
