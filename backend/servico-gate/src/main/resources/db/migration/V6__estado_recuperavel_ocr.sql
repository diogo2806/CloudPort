ALTER TABLE documento_agendamento
    ADD COLUMN IF NOT EXISTS tentativas_ocr INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS ultimo_erro_ocr VARCHAR(500),
    ADD COLUMN IF NOT EXISTS proxima_tentativa_ocr_em TIMESTAMP,
    ADD COLUMN IF NOT EXISTS processamento_ocr_iniciado_em TIMESTAMP;

UPDATE documento_agendamento
SET status_validacao = 'PENDENTE',
    mensagem_validacao = 'Documento aguardando processamento OCR.'
WHERE status_validacao = 'PROCESSANDO'
  AND ultima_revalidacao IS NULL;

CREATE INDEX IF NOT EXISTS idx_documento_agendamento_ocr_pendente
    ON documento_agendamento (status_validacao, updated_at);

CREATE INDEX IF NOT EXISTS idx_documento_agendamento_ocr_retry
    ON documento_agendamento (proxima_tentativa_ocr_em)
    WHERE status_validacao = 'FALHA';
