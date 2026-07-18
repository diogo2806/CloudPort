CREATE TABLE gate_resource_occupation (
    id BIGSERIAL PRIMARY KEY,
    gate_pass_id BIGINT NOT NULL REFERENCES gate_pass (id) ON DELETE CASCADE,
    tipo_recurso VARCHAR(20) NOT NULL,
    chave_recurso VARCHAR(120) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    ocupado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    liberado_em TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gate_resource_type CHECK (tipo_recurso IN ('MOTORISTA', 'CAVALO', 'CHASSIS', 'UNIDADE')),
    CONSTRAINT ck_gate_resource_release CHECK (
        (ativo = TRUE AND liberado_em IS NULL) OR (ativo = FALSE AND liberado_em IS NOT NULL)
    )
);

CREATE UNIQUE INDEX uk_gate_resource_occupation_active
    ON gate_resource_occupation (tipo_recurso, chave_recurso)
    WHERE ativo = TRUE;
CREATE INDEX idx_gate_resource_occupation_pass
    ON gate_resource_occupation (gate_pass_id, ativo);

CREATE TABLE gate_call (
    id BIGSERIAL PRIMARY KEY,
    gate_pass_id BIGINT NOT NULL REFERENCES gate_pass (id) ON DELETE CASCADE,
    status VARCHAR(30) NOT NULL,
    prioridade VARCHAR(20) NOT NULL,
    posicao_fila INTEGER NOT NULL,
    gate_pista VARCHAR(80) NOT NULL,
    chamado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    aceito_em TIMESTAMP WITHOUT TIME ZONE,
    expira_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    expirado_em TIMESTAMP WITHOUT TIME ZONE,
    atendimento_iniciado_em TIMESTAMP WITHOUT TIME ZONE,
    finalizado_em TIMESTAMP WITHOUT TIME ZONE,
    cancelado_em TIMESTAMP WITHOUT TIME ZONE,
    quantidade_rechamadas INTEGER NOT NULL DEFAULT 0,
    ultima_rechamada_em TIMESTAMP WITHOUT TIME ZONE,
    justificativa_cancelamento VARCHAR(500),
    operador VARCHAR(80),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gate_call_status CHECK (
        status IN ('CHAMADO', 'ACEITO', 'EM_ATENDIMENTO', 'FINALIZADO', 'EXPIRADO', 'CANCELADO')
    ),
    CONSTRAINT ck_gate_call_prioridade CHECK (prioridade IN ('NORMAL', 'ALTA', 'EMERGENCIAL')),
    CONSTRAINT ck_gate_call_position CHECK (posicao_fila > 0),
    CONSTRAINT ck_gate_call_recall CHECK (quantidade_rechamadas >= 0)
);

CREATE UNIQUE INDEX uk_gate_call_ativo
    ON gate_call (gate_pass_id)
    WHERE status IN ('CHAMADO', 'ACEITO', 'EM_ATENDIMENTO');
CREATE INDEX idx_gate_call_status ON gate_call (status);
CREATE INDEX idx_gate_call_prioridade ON gate_call (prioridade, posicao_fila, chamado_em);
CREATE INDEX idx_gate_call_expiration ON gate_call (expira_em) WHERE status = 'CHAMADO';

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
