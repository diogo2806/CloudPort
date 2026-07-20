CREATE TABLE gate_driver_credential (
    id BIGSERIAL PRIMARY KEY,
    motorista_id BIGINT NOT NULL REFERENCES motorista (id),
    transportadora_id BIGINT NOT NULL REFERENCES transportadora (id),
    tipo VARCHAR(20) NOT NULL,
    segredo_hash VARCHAR(128) NOT NULL,
    salt VARCHAR(64) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVA',
    validade_inicio TIMESTAMP WITHOUT TIME ZONE,
    validade_fim TIMESTAMP WITHOUT TIME ZONE,
    cadastrado_por VARCHAR(120) NOT NULL,
    revogado_por VARCHAR(120),
    revogado_em TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gate_driver_credential_tipo CHECK (tipo IN ('PIN', 'CREDENCIAL')),
    CONSTRAINT ck_gate_driver_credential_status CHECK (status IN ('ATIVA', 'REVOGADA', 'BLOQUEADA')),
    CONSTRAINT ck_gate_driver_credential_validade CHECK (
        validade_inicio IS NULL OR validade_fim IS NULL OR validade_fim > validade_inicio
    )
);

CREATE UNIQUE INDEX uk_gate_driver_credential_active
    ON gate_driver_credential (motorista_id, transportadora_id, tipo)
    WHERE status = 'ATIVA';

CREATE INDEX idx_gate_driver_credential_lookup
    ON gate_driver_credential (motorista_id, transportadora_id, tipo, status, validade_fim);

CREATE TABLE gate_driver_verification (
    id BIGSERIAL PRIMARY KEY,
    chave_operacional VARCHAR(80) NOT NULL UNIQUE,
    truck_visit_id BIGINT REFERENCES truck_visit (id) ON DELETE CASCADE,
    agendamento_id BIGINT REFERENCES agendamento (id) ON DELETE CASCADE,
    motorista_id BIGINT NOT NULL REFERENCES motorista (id),
    transportadora_id BIGINT NOT NULL REFERENCES transportadora (id),
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    metodo VARCHAR(20),
    tentativas INTEGER NOT NULL DEFAULT 0,
    limite_tentativas INTEGER NOT NULL DEFAULT 3,
    bloqueado_ate TIMESTAMP WITHOUT TIME ZONE,
    verificado_em TIMESTAMP WITHOUT TIME ZONE,
    expira_em TIMESTAMP WITHOUT TIME ZONE,
    verificado_por VARCHAR(120),
    override_por VARCHAR(120),
    motivo_override VARCHAR(500),
    ultimo_motivo VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gate_driver_verification_scope CHECK (
        truck_visit_id IS NOT NULL OR agendamento_id IS NOT NULL
    ),
    CONSTRAINT ck_gate_driver_verification_status CHECK (
        status IN ('PENDENTE', 'VERIFICADA', 'BLOQUEADA', 'EXPIRADA', 'OVERRIDE')
    ),
    CONSTRAINT ck_gate_driver_verification_metodo CHECK (
        metodo IS NULL OR metodo IN ('PIN', 'DOCUMENTO', 'CREDENCIAL', 'OVERRIDE')
    ),
    CONSTRAINT ck_gate_driver_verification_tentativas CHECK (
        tentativas >= 0 AND limite_tentativas > 0
    )
);

CREATE INDEX idx_gate_driver_verification_visit
    ON gate_driver_verification (truck_visit_id, status);
CREATE INDEX idx_gate_driver_verification_schedule
    ON gate_driver_verification (agendamento_id, status);
CREATE INDEX idx_gate_driver_verification_block
    ON gate_driver_verification (status, bloqueado_ate, expira_em);

CREATE TABLE gate_driver_verification_attempt (
    id BIGSERIAL PRIMARY KEY,
    verificacao_id BIGINT NOT NULL REFERENCES gate_driver_verification (id) ON DELETE CASCADE,
    metodo VARCHAR(20) NOT NULL,
    resultado VARCHAR(20) NOT NULL,
    motivo VARCHAR(500),
    operador VARCHAR(120) NOT NULL,
    ocorrido_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gate_driver_verification_attempt_method CHECK (
        metodo IN ('PIN', 'DOCUMENTO', 'CREDENCIAL', 'OVERRIDE')
    ),
    CONSTRAINT ck_gate_driver_verification_attempt_result CHECK (
        resultado IN ('APROVADA', 'NEGADA', 'BLOQUEADA', 'OVERRIDE')
    )
);

CREATE INDEX idx_gate_driver_verification_attempt_history
    ON gate_driver_verification_attempt (verificacao_id, ocorrido_em DESC);
