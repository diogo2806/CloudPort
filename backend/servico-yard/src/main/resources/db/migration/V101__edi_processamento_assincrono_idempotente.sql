ALTER TABLE edi_processamento
    ADD COLUMN identificador_interchange VARCHAR(100),
    ADD COLUMN identificador_mensagem VARCHAR(100),
    ADD COLUMN chave_idempotencia VARCHAR(64),
    ADD COLUMN hash_conteudo VARCHAR(64),
    ADD COLUMN proxima_tentativa_em TIMESTAMP;

ALTER TABLE edi_processamento
    ALTER COLUMN status TYPE VARCHAR(40),
    ALTER COLUMN tentativa SET DEFAULT 0;

ALTER TABLE edi_processamento
    DROP CONSTRAINT ck_edi_processamento_status,
    DROP CONSTRAINT ck_edi_processamento_tentativa;

UPDATE edi_processamento
   SET status = 'RECEBIDO'
 WHERE status = 'PROCESSANDO';

UPDATE edi_processamento
   SET hash_conteudo = md5(conteudo_original),
       chave_idempotencia = md5('LEGACY|' || id::TEXT),
       proxima_tentativa_em = CASE
           WHEN status = 'RECEBIDO' THEN CURRENT_TIMESTAMP
           ELSE NULL
       END;

ALTER TABLE edi_processamento
    ALTER COLUMN chave_idempotencia SET NOT NULL,
    ALTER COLUMN hash_conteudo SET NOT NULL;

ALTER TABLE edi_processamento
    ADD CONSTRAINT ck_edi_processamento_tentativa
        CHECK (tentativa >= 0),
    ADD CONSTRAINT ck_edi_processamento_status
        CHECK (status IN (
            'RECEBIDO',
            'PROCESSANDO',
            'AGUARDANDO_REPROCESSAMENTO',
            'CONCLUIDO',
            'REJEITADO',
            'QUARENTENA'
        )),
    ADD CONSTRAINT uk_edi_processamento_chave_idempotencia
        UNIQUE (chave_idempotencia);

CREATE UNIQUE INDEX uk_edi_processamento_identidade_natural
    ON edi_processamento (
        tipo_mensagem,
        identificador_interchange,
        identificador_mensagem,
        COALESCE(referencia_mensagem, '')
    )
    WHERE identificador_interchange IS NOT NULL
      AND identificador_mensagem IS NOT NULL;

CREATE INDEX idx_edi_processamento_fila
    ON edi_processamento(status, proxima_tentativa_em, criado_em);
