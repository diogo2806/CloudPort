CREATE TABLE transportadora (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    documento VARCHAR(20) NOT NULL UNIQUE,
    contato VARCHAR(120),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_transportadora_nome ON transportadora (nome);

CREATE TABLE motorista (
    id BIGSERIAL PRIMARY KEY,
    nome VARCHAR(120) NOT NULL,
    documento VARCHAR(20) NOT NULL,
    telefone VARCHAR(20),
    transportadora_id BIGINT NOT NULL REFERENCES transportadora (id),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_motorista_documento UNIQUE (documento, transportadora_id)
);

CREATE INDEX idx_motorista_transportadora ON motorista (transportadora_id);

CREATE TABLE veiculo (
    id BIGSERIAL PRIMARY KEY,
    placa VARCHAR(10) NOT NULL UNIQUE,
    modelo VARCHAR(60),
    tipo VARCHAR(40),
    transportadora_id BIGINT NOT NULL REFERENCES transportadora (id),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_veiculo_transportadora ON veiculo (transportadora_id);

CREATE TABLE janela_atendimento (
    id BIGSERIAL PRIMARY KEY,
    data DATE NOT NULL,
    hora_inicio TIME WITHOUT TIME ZONE NOT NULL,
    hora_fim TIME WITHOUT TIME ZONE NOT NULL,
    capacidade INTEGER NOT NULL,
    canal_entrada VARCHAR(40) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX uk_janela_atendimento_slot ON janela_atendimento (data, hora_inicio, canal_entrada);

CREATE TABLE agendamento (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    tipo_operacao VARCHAR(40) NOT NULL,
    status VARCHAR(40) NOT NULL,
    transportadora_id BIGINT NOT NULL REFERENCES transportadora (id),
    motorista_id BIGINT NOT NULL REFERENCES motorista (id),
    veiculo_id BIGINT NOT NULL REFERENCES veiculo (id),
    janela_atendimento_id BIGINT NOT NULL REFERENCES janela_atendimento (id),
    horario_previsto_chegada TIMESTAMP WITHOUT TIME ZONE,
    horario_previsto_saida TIMESTAMP WITHOUT TIME ZONE,
    horario_real_chegada TIMESTAMP WITHOUT TIME ZONE,
    horario_real_saida TIMESTAMP WITHOUT TIME ZONE,
    observacoes VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_agendamento_status ON agendamento (status);
CREATE INDEX idx_agendamento_janela ON agendamento (janela_atendimento_id);
CREATE INDEX idx_agendamento_transportadora ON agendamento (transportadora_id);

CREATE TABLE documento_agendamento (
    id BIGSERIAL PRIMARY KEY,
    tipo_documento VARCHAR(80) NOT NULL,
    numero VARCHAR(80),
    url_documento VARCHAR(255),
    agendamento_id BIGINT NOT NULL REFERENCES agendamento (id) ON DELETE CASCADE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_documento_agendamento_agendamento ON documento_agendamento (agendamento_id);

CREATE TABLE gate_pass (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    status VARCHAR(40) NOT NULL,
    data_entrada TIMESTAMP WITHOUT TIME ZONE,
    data_saida TIMESTAMP WITHOUT TIME ZONE,
    agendamento_id BIGINT NOT NULL UNIQUE REFERENCES agendamento (id),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gate_pass_status ON gate_pass (status);

CREATE TABLE gate_event (
    id BIGSERIAL PRIMARY KEY,
    status VARCHAR(40) NOT NULL,
    motivo_excecao VARCHAR(40),
    observacao VARCHAR(500),
    usuario_responsavel VARCHAR(80),
    registrado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    gate_pass_id BIGINT NOT NULL REFERENCES gate_pass (id) ON DELETE CASCADE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_gate_event_gate_pass ON gate_event (gate_pass_id);
CREATE INDEX idx_gate_event_status ON gate_event (status);
