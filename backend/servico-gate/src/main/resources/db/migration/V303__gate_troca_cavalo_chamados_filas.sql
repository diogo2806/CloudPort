CREATE TABLE truck_hopping_session (
    id BIGSERIAL PRIMARY KEY,
    cpf_motorista VARCHAR(14) NOT NULL,
    numero_cnh VARCHAR(20) NOT NULL,
    cavalo_atual VARCHAR(10) NOT NULL,
    status VARCHAR(20) NOT NULL,
    gate_in_id BIGINT REFERENCES gate_pass (id),
    gate_out_id BIGINT REFERENCES gate_pass (id),
    encerrada_em TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_truck_hopping_status CHECK (status IN ('ABERTA', 'ENCERRADA'))
);

CREATE UNIQUE INDEX uk_truck_hopping_cpf_aberta
    ON truck_hopping_session (cpf_motorista)
    WHERE status = 'ABERTA';
CREATE INDEX idx_truck_hopping_status ON truck_hopping_session (status);
CREATE INDEX idx_truck_hopping_gate_in ON truck_hopping_session (gate_in_id);

CREATE TABLE gate_call (
    id BIGSERIAL PRIMARY KEY,
    gate_pass_id BIGINT NOT NULL REFERENCES gate_pass (id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    prioridade VARCHAR(20) NOT NULL,
    chamado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atendimento_iniciado_em TIMESTAMP WITHOUT TIME ZONE,
    finalizado_em TIMESTAMP WITHOUT TIME ZONE,
    cancelado_em TIMESTAMP WITHOUT TIME ZONE,
    justificativa_cancelamento VARCHAR(500),
    operador VARCHAR(80),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gate_call_status CHECK (status IN ('CHAMADO', 'EM_ATENDIMENTO', 'FINALIZADO', 'CANCELADO')),
    CONSTRAINT ck_gate_call_prioridade CHECK (prioridade IN ('NORMAL', 'ALTA', 'EMERGENCIAL'))
);

CREATE UNIQUE INDEX uk_gate_call_ativo
    ON gate_call (gate_pass_id)
    WHERE status IN ('CHAMADO', 'EM_ATENDIMENTO');
CREATE INDEX idx_gate_call_status ON gate_call (status);
CREATE INDEX idx_gate_call_prioridade ON gate_call (prioridade, chamado_em);

CREATE TABLE gate_queue_entry (
    id BIGSERIAL PRIMARY KEY,
    gate_pass_id BIGINT NOT NULL REFERENCES gate_pass (id) ON DELETE CASCADE,
    sentido VARCHAR(10) NOT NULL,
    status VARCHAR(30) NOT NULL,
    posicao_original INTEGER NOT NULL,
    posicao_atual INTEGER NOT NULL,
    prioridade VARCHAR(20) NOT NULL,
    justificativa_prioridade VARCHAR(500),
    operador_prioridade VARCHAR(80),
    entrou_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    chamado_em TIMESTAMP WITHOUT TIME ZONE,
    atendimento_iniciado_em TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gate_queue_sentido CHECK (sentido IN ('ENTRADA', 'SAIDA')),
    CONSTRAINT ck_gate_queue_status CHECK (status IN ('AGUARDANDO', 'CHAMADO', 'EM_ATENDIMENTO')),
    CONSTRAINT ck_gate_queue_prioridade CHECK (prioridade IN ('NORMAL', 'ALTA', 'EMERGENCIAL'))
);

CREATE UNIQUE INDEX uk_gate_queue_ativo
    ON gate_queue_entry (gate_pass_id, sentido)
    WHERE status IN ('AGUARDANDO', 'CHAMADO', 'EM_ATENDIMENTO');
CREATE INDEX idx_gate_queue_ordem
    ON gate_queue_entry (sentido, prioridade, posicao_atual, entrou_em);
