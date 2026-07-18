CREATE TABLE work_instruction (
    id BIGSERIAL PRIMARY KEY,
    codigo_conteiner VARCHAR(40) NOT NULL,
    tipo_operacao VARCHAR(30) NOT NULL,
    origem VARCHAR(120),
    destino VARCHAR(120),
    prioridade VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    agendada_em TIMESTAMP WITHOUT TIME ZONE,
    aceita_em TIMESTAMP WITHOUT TIME ZONE,
    iniciada_em TIMESTAMP WITHOUT TIME ZONE,
    concluida_em TIMESTAMP WITHOUT TIME ZONE,
    falha_em TIMESTAMP WITHOUT TIME ZONE,
    cancelada_em TIMESTAMP WITHOUT TIME ZONE,
    equipamento VARCHAR(40),
    equipe VARCHAR(80),
    observacoes VARCHAR(1000),
    criado_por VARCHAR(80) NOT NULL,
    justificativa_cancelamento VARCHAR(500),
    resultado_vmt VARCHAR(1000),
    versao BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_work_instruction_operacao CHECK (
        tipo_operacao IN ('MOVIMENTACAO', 'BLOQUEIO', 'DESBLOQUEIO', 'INSPECAO')
    ),
    CONSTRAINT ck_work_instruction_prioridade CHECK (
        prioridade IN ('NORMAL', 'ALTA', 'EMERGENCIAL')
    ),
    CONSTRAINT ck_work_instruction_status CHECK (
        status IN ('PENDENTE', 'ACEITA', 'EM_EXECUCAO', 'CONCLUIDA', 'FALHA', 'CANCELADA')
    )
);

CREATE INDEX idx_work_instruction_status ON work_instruction (status, prioridade, agendada_em);
CREATE INDEX idx_work_instruction_conteiner ON work_instruction (codigo_conteiner);
CREATE UNIQUE INDEX uk_work_instruction_destino_ativo
    ON work_instruction (destino)
    WHERE destino IS NOT NULL
      AND tipo_operacao = 'MOVIMENTACAO'
      AND status IN ('PENDENTE', 'ACEITA', 'EM_EXECUCAO');

CREATE TABLE yard_position_divergence (
    id BIGSERIAL PRIMARY KEY,
    unidade_id BIGINT NOT NULL REFERENCES unidade_inventario(id),
    identificacao_unidade VARCHAR(40) NOT NULL,
    posicao_esperada VARCHAR(120) NOT NULL,
    posicao_encontrada VARCHAR(120) NOT NULL,
    status VARCHAR(30) NOT NULL,
    bloqueada BOOLEAN NOT NULL DEFAULT TRUE,
    responsavel VARCHAR(120),
    evidencia VARCHAR(1000),
    decisao VARCHAR(1000),
    instrucao_corretiva_id BIGINT REFERENCES work_instruction(id),
    aberta_por VARCHAR(120) NOT NULL,
    aberta_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    investigacao_iniciada_em TIMESTAMP WITHOUT TIME ZONE,
    resolvida_em TIMESTAMP WITHOUT TIME ZONE,
    cancelada_em TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_yard_position_divergence_status CHECK (
        status IN ('ABERTA', 'EM_INVESTIGACAO', 'CORRECAO_PENDENTE', 'RESOLVIDA', 'CANCELADA')
    ),
    CONSTRAINT ck_yard_position_divergence_positions CHECK (posicao_esperada <> posicao_encontrada)
);

CREATE UNIQUE INDEX uk_yard_position_divergence_active
    ON yard_position_divergence (unidade_id)
    WHERE status IN ('ABERTA', 'EM_INVESTIGACAO', 'CORRECAO_PENDENTE');
CREATE INDEX idx_yard_position_divergence_status
    ON yard_position_divergence (status, aberta_em DESC);

CREATE TABLE lost_found_case (
    id BIGSERIAL PRIMARY KEY,
    identificacao_lida VARCHAR(40) NOT NULL,
    unidade_id BIGINT REFERENCES unidade_inventario(id),
    tipo_caso VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    responsavel VARCHAR(120),
    evidencia VARCHAR(2000),
    decisao_final VARCHAR(1000),
    aberto_por VARCHAR(120) NOT NULL,
    aberto_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    investigacao_iniciada_em TIMESTAMP WITHOUT TIME ZONE,
    associada_em TIMESTAMP WITHOUT TIME ZONE,
    regularizada_em TIMESTAMP WITHOUT TIME ZONE,
    baixada_em TIMESTAMP WITHOUT TIME ZONE,
    encerrada_em TIMESTAMP WITHOUT TIME ZONE,
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_lost_found_case_type CHECK (
        tipo_caso IN ('SEM_REGISTRO', 'NAO_LOCALIZADA', 'TBD')
    ),
    CONSTRAINT ck_lost_found_case_status CHECK (
        status IN ('ABERTO', 'EM_INVESTIGACAO', 'ASSOCIADO', 'REGULARIZADO', 'BAIXADO', 'ENCERRADO')
    )
);

CREATE UNIQUE INDEX uk_lost_found_case_active
    ON lost_found_case (identificacao_lida)
    WHERE status NOT IN ('REGULARIZADO', 'BAIXADO', 'ENCERRADO');
CREATE INDEX idx_lost_found_case_queue
    ON lost_found_case (status, aberto_em DESC);

CREATE TABLE vmt_instruction_event (
    id BIGSERIAL PRIMARY KEY,
    event_id VARCHAR(120) NOT NULL UNIQUE,
    instruction_id BIGINT NOT NULL REFERENCES work_instruction(id) ON DELETE CASCADE,
    event_type VARCHAR(30) NOT NULL,
    expected_status VARCHAR(30) NOT NULL,
    occurred_at TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    result VARCHAR(1000),
    payload TEXT,
    processed_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_vmt_instruction_event_type CHECK (
        event_type IN ('ACEITE', 'INICIO', 'FALHA', 'CONCLUSAO')
    )
);

CREATE INDEX idx_vmt_instruction_event_instruction
    ON vmt_instruction_event (instruction_id, processed_at);
