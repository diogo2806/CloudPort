ALTER TABLE equipamento_patio
    ADD COLUMN IF NOT EXISTS atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW();

CREATE TABLE work_instruction (
    id BIGSERIAL PRIMARY KEY,
    codigo_conteiner VARCHAR(30) NOT NULL,
    tipo_operacao VARCHAR(30) NOT NULL,
    origem VARCHAR(80),
    destino VARCHAR(80),
    prioridade VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL,
    agendada_em TIMESTAMP WITHOUT TIME ZONE,
    iniciada_em TIMESTAMP WITHOUT TIME ZONE,
    concluida_em TIMESTAMP WITHOUT TIME ZONE,
    cancelada_em TIMESTAMP WITHOUT TIME ZONE,
    equipamento VARCHAR(30),
    equipe VARCHAR(80),
    observacoes VARCHAR(1000),
    criado_por VARCHAR(80) NOT NULL,
    justificativa_cancelamento VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_work_instruction_operacao CHECK (
        tipo_operacao IN ('MOVIMENTACAO', 'BLOQUEIO', 'DESBLOQUEIO', 'INSPECAO')
    ),
    CONSTRAINT ck_work_instruction_prioridade CHECK (
        prioridade IN ('NORMAL', 'ALTA', 'EMERGENCIAL')
    ),
    CONSTRAINT ck_work_instruction_status CHECK (
        status IN ('PENDENTE', 'EM_EXECUCAO', 'CONCLUIDA', 'CANCELADA')
    )
);

CREATE INDEX idx_work_instruction_status ON work_instruction (status, prioridade, agendada_em);
CREATE INDEX idx_work_instruction_conteiner ON work_instruction (codigo_conteiner);
CREATE UNIQUE INDEX uk_work_instruction_destino_ativo
    ON work_instruction (destino)
    WHERE destino IS NOT NULL
      AND tipo_operacao = 'MOVIMENTACAO'
      AND status IN ('PENDENTE', 'EM_EXECUCAO');
