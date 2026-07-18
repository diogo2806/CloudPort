CREATE TABLE gate_bill_of_lading (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(80) NOT NULL UNIQUE,
    armador VARCHAR(120),
    viagem VARCHAR(80),
    consignatario VARCHAR(160),
    quantidade_total INTEGER NOT NULL DEFAULT 1,
    quantidade_liberada INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ATIVO',
    validade_inicio TIMESTAMP WITHOUT TIME ZONE,
    validade_fim TIMESTAMP WITHOUT TIME ZONE,
    observacoes VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gate_bl_quantidade CHECK (
        quantidade_total > 0
        AND quantidade_liberada >= 0
        AND quantidade_liberada <= quantidade_total
    ),
    CONSTRAINT ck_gate_bl_status CHECK (status IN ('ATIVO', 'PARCIAL', 'LIBERADO', 'CANCELADO', 'EXPIRADO'))
);

ALTER TABLE gate_order
    ADD COLUMN bill_of_lading_id BIGINT REFERENCES gate_bill_of_lading (id);

ALTER TABLE gate_transaction
    ADD COLUMN bill_of_lading_id BIGINT REFERENCES gate_bill_of_lading (id);

CREATE TABLE gate_access_rule (
    id BIGSERIAL PRIMARY KEY,
    gate_id BIGINT NOT NULL REFERENCES gate_configuracao (id) ON DELETE CASCADE,
    escopo VARCHAR(30) NOT NULL,
    referencia_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    inicio_vigencia TIMESTAMP WITHOUT TIME ZONE,
    fim_vigencia TIMESTAMP WITHOUT TIME ZONE,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gate_access_rule UNIQUE (gate_id, escopo, referencia_id, tipo),
    CONSTRAINT ck_gate_access_rule_escopo CHECK (escopo IN ('MOTORISTA', 'TRANSPORTADORA', 'VEICULO')),
    CONSTRAINT ck_gate_access_rule_tipo CHECK (tipo IN ('BLOQUEIO', 'PERMISSAO')),
    CONSTRAINT ck_gate_access_rule_vigencia CHECK (
        inicio_vigencia IS NULL OR fim_vigencia IS NULL OR fim_vigencia >= inicio_vigencia
    )
);

CREATE UNIQUE INDEX uk_truck_visit_agendamento_ativo
    ON truck_visit (agendamento_id)
    WHERE agendamento_id IS NOT NULL
      AND status IN ('PREVISTA', 'CHECKIN', 'EM_PROCESSAMENTO', 'TROUBLE');

CREATE INDEX idx_gate_bl_status
    ON gate_bill_of_lading (status, validade_fim);

CREATE INDEX idx_gate_order_bl
    ON gate_order (bill_of_lading_id);

CREATE INDEX idx_gate_transaction_bl
    ON gate_transaction (bill_of_lading_id);

CREATE INDEX idx_gate_access_rule_busca
    ON gate_access_rule (gate_id, escopo, referencia_id, ativo, inicio_vigencia, fim_vigencia);