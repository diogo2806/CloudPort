ALTER TABLE documento_agendamento ADD COLUMN status_validacao VARCHAR(30);
ALTER TABLE documento_agendamento ADD COLUMN mensagem_validacao VARCHAR(500);

UPDATE documento_agendamento
SET status_validacao = CASE
        WHEN ultima_revalidacao IS NOT NULL THEN 'VALIDADO'
        ELSE 'PENDENTE'
    END,
    mensagem_validacao = CASE
        WHEN ultima_revalidacao IS NOT NULL THEN 'Documento validado manualmente antes da automação.'
        ELSE 'Documento aguardando validação automática.'
    END;

ALTER TABLE documento_agendamento ALTER COLUMN status_validacao SET NOT NULL;
