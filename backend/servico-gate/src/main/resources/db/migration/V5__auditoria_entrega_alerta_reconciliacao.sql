ALTER TABLE reconciliacao_barcode
    ADD COLUMN IF NOT EXISTS status_entrega_alerta VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    ADD COLUMN IF NOT EXISTS alerta_enviado_em TIMESTAMP,
    ADD COLUMN IF NOT EXISTS alerta_canal VARCHAR(40),
    ADD COLUMN IF NOT EXISTS alerta_identificador_externo VARCHAR(120),
    ADD COLUMN IF NOT EXISTS alerta_tentativas INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS alerta_ultimo_erro VARCHAR(1000);

UPDATE reconciliacao_barcode
SET status_entrega_alerta = CASE WHEN alerta_enviado THEN 'ENVIADO' ELSE 'PENDENTE' END,
    alerta_enviado_em = CASE WHEN alerta_enviado THEN COALESCE(updated_at, detectado_em) ELSE NULL END
WHERE status_entrega_alerta IS NULL
   OR (alerta_enviado AND status_entrega_alerta <> 'ENVIADO');

CREATE INDEX IF NOT EXISTS idx_reconciliacao_barcode_alerta_pendente
    ON reconciliacao_barcode (alerta_enviado, resolvido_em, detectado_em);
