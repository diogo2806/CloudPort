CREATE SEQUENCE IF NOT EXISTS billing_fatura_numero_seq START WITH 1 INCREMENT BY 1;

CREATE TABLE IF NOT EXISTS billing_tarifa (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(50) NOT NULL UNIQUE,
    descricao VARCHAR(160) NOT NULL,
    tipo_operacao VARCHAR(40),
    valor NUMERIC(15, 2) NOT NULL,
    inicio_vigencia DATE NOT NULL,
    fim_vigencia DATE,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_billing_tarifa_valor CHECK (valor >= 0),
    CONSTRAINT ck_billing_tarifa_vigencia CHECK (fim_vigencia IS NULL OR fim_vigencia >= inicio_vigencia)
);

CREATE INDEX IF NOT EXISTS idx_billing_tarifa_operacao_vigencia
    ON billing_tarifa (tipo_operacao, ativa, inicio_vigencia, fim_vigencia);

CREATE TABLE IF NOT EXISTS billing_cobranca (
    id BIGSERIAL PRIMARY KEY,
    referencia VARCHAR(100) NOT NULL UNIQUE,
    transportadora_id BIGINT NOT NULL REFERENCES transportadora (id),
    agendamento_id BIGINT REFERENCES agendamento (id),
    tarifa_id BIGINT NOT NULL REFERENCES billing_tarifa (id),
    descricao VARCHAR(200) NOT NULL,
    valor NUMERIC(15, 2) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    ocorrido_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    faturado_em TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_billing_cobranca_valor CHECK (valor >= 0),
    CONSTRAINT ck_billing_cobranca_status CHECK (status IN ('PENDENTE', 'FATURADA', 'CANCELADA'))
);

CREATE INDEX IF NOT EXISTS idx_billing_cobranca_transportadora_status
    ON billing_cobranca (transportadora_id, status, ocorrido_em DESC);
CREATE INDEX IF NOT EXISTS idx_billing_cobranca_agendamento
    ON billing_cobranca (agendamento_id);

CREATE TABLE IF NOT EXISTS billing_fatura (
    id BIGSERIAL PRIMARY KEY,
    numero VARCHAR(40) NOT NULL UNIQUE,
    transportadora_id BIGINT NOT NULL REFERENCES transportadora (id),
    emitida_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    vencimento DATE NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ABERTA',
    subtotal NUMERIC(15, 2) NOT NULL,
    total NUMERIC(15, 2) NOT NULL,
    pago_em TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_billing_fatura_valores CHECK (subtotal >= 0 AND total >= 0),
    CONSTRAINT ck_billing_fatura_status CHECK (status IN ('ABERTA', 'PAGA', 'CANCELADA'))
);

CREATE INDEX IF NOT EXISTS idx_billing_fatura_transportadora_status
    ON billing_fatura (transportadora_id, status, emitida_em DESC);

CREATE TABLE IF NOT EXISTS billing_fatura_item (
    id BIGSERIAL PRIMARY KEY,
    fatura_id BIGINT NOT NULL REFERENCES billing_fatura (id) ON DELETE CASCADE,
    cobranca_id BIGINT NOT NULL UNIQUE REFERENCES billing_cobranca (id),
    descricao VARCHAR(200) NOT NULL,
    valor NUMERIC(15, 2) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_billing_fatura_item_valor CHECK (valor >= 0)
);

CREATE INDEX IF NOT EXISTS idx_billing_fatura_item_fatura
    ON billing_fatura_item (fatura_id);

CREATE TABLE IF NOT EXISTS billing_pagamento (
    id BIGSERIAL PRIMARY KEY,
    fatura_id BIGINT NOT NULL REFERENCES billing_fatura (id),
    valor NUMERIC(15, 2) NOT NULL,
    forma VARCHAR(40) NOT NULL,
    referencia VARCHAR(100),
    pago_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_billing_pagamento_valor CHECK (valor > 0)
);

CREATE INDEX IF NOT EXISTS idx_billing_pagamento_fatura
    ON billing_pagamento (fatura_id, pago_em DESC);
