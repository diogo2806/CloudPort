ALTER TABLE edi_processamento
    ADD COLUMN identificador_unb VARCHAR(100),
    ADD COLUMN identificador_unh VARCHAR(100),
    ADD COLUMN chave_idempotencia VARCHAR(64),
    ADD COLUMN hash_conteudo VARCHAR(64),
    ADD COLUMN proxima_tentativa_em TIMESTAMP,
    ADD COLUMN processando_desde TIMESTAMP;

UPDATE edi_processamento
SET chave_idempotencia = md5('LEGACY|' || id::text),
    hash_conteudo = md5(conteudo_original)
WHERE chave_idempotencia IS NULL;

UPDATE edi_processamento
SET proxima_tentativa_em = COALESCE(atualizado_em, CURRENT_TIMESTAMP)
WHERE status = 'RECEBIDO';

UPDATE edi_processamento
SET processando_desde = COALESCE(atualizado_em, CURRENT_TIMESTAMP)
WHERE status = 'PROCESSANDO';

ALTER TABLE edi_processamento
    ALTER COLUMN chave_idempotencia SET NOT NULL,
    ALTER COLUMN hash_conteudo SET NOT NULL,
    ALTER COLUMN status TYPE VARCHAR(30);

ALTER TABLE edi_processamento
    DROP CONSTRAINT IF EXISTS ck_edi_processamento_status;

ALTER TABLE edi_processamento
    ADD CONSTRAINT ck_edi_processamento_status CHECK (
        status IN (
            'RECEBIDO',
            'PROCESSANDO',
            'AGUARDANDO_RETENTATIVA',
            'CONCLUIDO',
            'REJEITADO',
            'QUARENTENA'
        )
    );

CREATE UNIQUE INDEX uq_edi_processamento_chave_idempotencia
    ON edi_processamento(chave_idempotencia);

CREATE INDEX idx_edi_processamento_fila
    ON edi_processamento(status, proxima_tentativa_em, criado_em);

CREATE INDEX idx_edi_processamento_travado
    ON edi_processamento(status, processando_desde)
    WHERE status = 'PROCESSANDO';
