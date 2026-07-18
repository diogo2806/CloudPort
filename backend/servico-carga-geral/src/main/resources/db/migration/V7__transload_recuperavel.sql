ALTER TABLE operacao_transload
    DROP CONSTRAINT ck_operacao_transload_status;

ALTER TABLE operacao_transload
    ADD COLUMN motivo_cancelamento VARCHAR(1000),
    ADD COLUMN criado_em TIMESTAMP WITH TIME ZONE,
    ADD COLUMN atualizado_em TIMESTAMP WITH TIME ZONE;

UPDATE operacao_transload
SET criado_em = executado_em,
    atualizado_em = executado_em;

ALTER TABLE operacao_transload
    ALTER COLUMN criado_em SET NOT NULL,
    ALTER COLUMN atualizado_em SET NOT NULL,
    ALTER COLUMN executado_em DROP NOT NULL;

ALTER TABLE operacao_transload
    ADD CONSTRAINT ck_operacao_transload_status
        CHECK (status IN ('EM_EXECUCAO', 'CONCLUIDO', 'CANCELADO'));

CREATE INDEX idx_operacao_transload_status_atualizacao
    ON operacao_transload(status, atualizado_em DESC);
