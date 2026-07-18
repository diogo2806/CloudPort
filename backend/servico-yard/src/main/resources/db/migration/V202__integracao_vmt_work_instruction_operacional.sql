ALTER TABLE ordem_trabalho_patio
    ADD COLUMN status_confirmacao_vmt VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    ADD COLUMN vmt_aceito_em TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN vmt_iniciado_em TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN vmt_falha_em TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN vmt_concluido_em TIMESTAMP WITHOUT TIME ZONE,
    ADD COLUMN ultimo_evento_vmt_id VARCHAR(120),
    ADD COLUMN resultado_vmt VARCHAR(1000);

ALTER TABLE ordem_trabalho_patio
    ADD CONSTRAINT ck_ordem_trabalho_patio_status_vmt CHECK (
        status_confirmacao_vmt IN ('PENDENTE', 'ACEITA', 'EM_EXECUCAO', 'FALHA', 'CONCLUIDA')
    );

CREATE TABLE evento_vmt_work_instruction (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(120) NOT NULL,
    ordem_trabalho_patio_id BIGINT NOT NULL REFERENCES ordem_trabalho_patio(id) ON DELETE CASCADE,
    tipo_evento VARCHAR(30) NOT NULL,
    status_esperado VARCHAR(30) NOT NULL,
    status_resultante VARCHAR(30) NOT NULL,
    ocorrido_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    resultado VARCHAR(1000),
    payload TEXT,
    processado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_evento_vmt_work_instruction_event_id UNIQUE (event_id),
    CONSTRAINT ck_evento_vmt_work_instruction_tipo CHECK (
        tipo_evento IN ('ACEITE', 'INICIO', 'FALHA', 'CONCLUSAO')
    ),
    CONSTRAINT ck_evento_vmt_work_instruction_status_esperado CHECK (
        status_esperado IN ('PENDENTE', 'ACEITA', 'EM_EXECUCAO', 'FALHA', 'CONCLUIDA')
    ),
    CONSTRAINT ck_evento_vmt_work_instruction_status_resultante CHECK (
        status_resultante IN ('PENDENTE', 'ACEITA', 'EM_EXECUCAO', 'FALHA', 'CONCLUIDA')
    )
);

CREATE INDEX idx_evento_vmt_work_instruction_ciclo
    ON evento_vmt_work_instruction (ordem_trabalho_patio_id, ocorrido_em, processado_em);

CREATE INDEX idx_ordem_trabalho_patio_status_vmt
    ON ordem_trabalho_patio (status_confirmacao_vmt, atualizado_em);
