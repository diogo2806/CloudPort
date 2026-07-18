CREATE TABLE gate_facility (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    nome VARCHAR(120) NOT NULL,
    fuso_horario VARCHAR(60) NOT NULL DEFAULT 'America/Sao_Paulo',
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW()
);

CREATE TABLE gate_configuracao (
    id BIGSERIAL PRIMARY KEY,
    facility_id BIGINT NOT NULL REFERENCES gate_facility (id),
    codigo VARCHAR(40) NOT NULL,
    nome VARCHAR(120) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gate_configuracao UNIQUE (facility_id, codigo)
);

CREATE TABLE gate_lane (
    id BIGSERIAL PRIMARY KEY,
    gate_id BIGINT NOT NULL REFERENCES gate_configuracao (id),
    codigo VARCHAR(40) NOT NULL,
    nome VARCHAR(120) NOT NULL,
    direcao VARCHAR(20) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'ABERTA',
    capacidade_fila INTEGER NOT NULL DEFAULT 1,
    ocr_habilitado BOOLEAN NOT NULL DEFAULT FALSE,
    balanca_habilitada BOOLEAN NOT NULL DEFAULT FALSE,
    inspecao_habilitada BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gate_lane UNIQUE (gate_id, codigo),
    CONSTRAINT ck_gate_lane_direcao CHECK (direcao IN ('ENTRADA', 'SAIDA', 'BIDIRECIONAL')),
    CONSTRAINT ck_gate_lane_status CHECK (status IN ('ABERTA', 'FECHADA', 'MANUTENCAO')),
    CONSTRAINT ck_gate_lane_capacidade CHECK (capacidade_fila > 0)
);

CREATE TABLE gate_stage_config (
    id BIGSERIAL PRIMARY KEY,
    gate_id BIGINT NOT NULL REFERENCES gate_configuracao (id),
    codigo VARCHAR(40) NOT NULL,
    nome VARCHAR(120) NOT NULL,
    ordem INTEGER NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    permite_trouble BOOLEAN NOT NULL DEFAULT TRUE,
    finaliza_visita BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gate_stage_codigo UNIQUE (gate_id, codigo),
    CONSTRAINT uk_gate_stage_ordem UNIQUE (gate_id, ordem)
);

CREATE TABLE gate_stage_transition (
    id BIGSERIAL PRIMARY KEY,
    origem_stage_id BIGINT NOT NULL REFERENCES gate_stage_config (id),
    destino_stage_id BIGINT NOT NULL REFERENCES gate_stage_config (id),
    condicao VARCHAR(500),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gate_stage_transition UNIQUE (origem_stage_id, destino_stage_id),
    CONSTRAINT ck_gate_stage_transition_distinta CHECK (origem_stage_id <> destino_stage_id)
);

CREATE TABLE gate_business_task (
    id BIGSERIAL PRIMARY KEY,
    stage_id BIGINT NOT NULL REFERENCES gate_stage_config (id) ON DELETE CASCADE,
    codigo VARCHAR(60) NOT NULL,
    nome VARCHAR(160) NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    ordem INTEGER NOT NULL,
    obrigatoria BOOLEAN NOT NULL DEFAULT TRUE,
    configuracao JSONB NOT NULL DEFAULT '{}'::JSONB,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gate_business_task UNIQUE (stage_id, codigo),
    CONSTRAINT ck_gate_business_task_tipo CHECK (tipo IN ('VALIDACAO', 'CAPTURA', 'INTEGRACAO', 'IMPRESSAO', 'DECISAO'))
);

CREATE TABLE gate_booking (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    transportadora_id BIGINT REFERENCES transportadora (id),
    armador VARCHAR(120),
    viagem VARCHAR(80),
    quantidade_total INTEGER NOT NULL DEFAULT 1,
    quantidade_utilizada INTEGER NOT NULL DEFAULT 0,
    status VARCHAR(30) NOT NULL DEFAULT 'ABERTO',
    validade_inicio TIMESTAMP WITHOUT TIME ZONE,
    validade_fim TIMESTAMP WITHOUT TIME ZONE,
    observacoes VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gate_booking_quantidade CHECK (quantidade_total > 0 AND quantidade_utilizada >= 0 AND quantidade_utilizada <= quantidade_total),
    CONSTRAINT ck_gate_booking_status CHECK (status IN ('ABERTO', 'PARCIAL', 'UTILIZADO', 'CANCELADO', 'EXPIRADO'))
);

CREATE TABLE gate_order (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(20) NOT NULL,
    codigo VARCHAR(60) NOT NULL,
    booking_id BIGINT REFERENCES gate_booking (id),
    transportadora_id BIGINT REFERENCES transportadora (id),
    unidade_referencia VARCHAR(30),
    status VARCHAR(30) NOT NULL DEFAULT 'ATIVA',
    validade_inicio TIMESTAMP WITHOUT TIME ZONE,
    validade_fim TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gate_order UNIQUE (tipo, codigo),
    CONSTRAINT ck_gate_order_tipo CHECK (tipo IN ('EDO', 'ERO', 'IDO')),
    CONSTRAINT ck_gate_order_status CHECK (status IN ('ATIVA', 'UTILIZADA', 'CANCELADA', 'EXPIRADA'))
);

CREATE TABLE gate_preadvice (
    id BIGSERIAL PRIMARY KEY,
    tipo VARCHAR(30) NOT NULL,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    booking_id BIGINT REFERENCES gate_booking (id),
    order_id BIGINT REFERENCES gate_order (id),
    unidade_referencia VARCHAR(30),
    iso_type VARCHAR(10),
    peso_bruto_kg NUMERIC(12, 3),
    status VARCHAR(30) NOT NULL DEFAULT 'ATIVO',
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gate_preadvice_tipo CHECK (tipo IN ('EXPORTACAO', 'VAZIO')),
    CONSTRAINT ck_gate_preadvice_status CHECK (status IN ('ATIVO', 'UTILIZADO', 'CANCELADO', 'EXPIRADO'))
);

ALTER TABLE janela_atendimento
    ADD COLUMN gate_id BIGINT REFERENCES gate_configuracao (id),
    ADD COLUMN capacidade_utilizada INTEGER NOT NULL DEFAULT 0,
    ADD COLUMN tolerancia_antecipada_minutos INTEGER NOT NULL DEFAULT 60,
    ADD COLUMN tolerancia_atraso_minutos INTEGER NOT NULL DEFAULT 60;

ALTER TABLE agendamento
    ADD COLUMN facility_id BIGINT REFERENCES gate_facility (id),
    ADD COLUMN gate_id BIGINT REFERENCES gate_configuracao (id),
    ADD COLUMN booking_id BIGINT REFERENCES gate_booking (id),
    ADD COLUMN order_id BIGINT REFERENCES gate_order (id);

CREATE TABLE truck_visit (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    agendamento_id BIGINT REFERENCES agendamento (id),
    facility_id BIGINT NOT NULL REFERENCES gate_facility (id),
    gate_id BIGINT NOT NULL REFERENCES gate_configuracao (id),
    lane_id BIGINT REFERENCES gate_lane (id),
    transportadora_id BIGINT NOT NULL REFERENCES transportadora (id),
    motorista_id BIGINT NOT NULL REFERENCES motorista (id),
    veiculo_id BIGINT NOT NULL REFERENCES veiculo (id),
    stage_atual_id BIGINT REFERENCES gate_stage_config (id),
    status VARCHAR(30) NOT NULL DEFAULT 'PREVISTA',
    checkin_em TIMESTAMP WITHOUT TIME ZONE,
    checkout_em TIMESTAMP WITHOUT TIME ZONE,
    iniciado_em TIMESTAMP WITHOUT TIME ZONE,
    finalizado_em TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_truck_visit_status CHECK (status IN ('PREVISTA', 'CHECKIN', 'EM_PROCESSAMENTO', 'TROUBLE', 'FINALIZADA', 'CANCELADA'))
);

CREATE TABLE gate_transaction (
    id BIGSERIAL PRIMARY KEY,
    truck_visit_id BIGINT NOT NULL REFERENCES truck_visit (id) ON DELETE CASCADE,
    sequencia INTEGER NOT NULL,
    tipo_operacao VARCHAR(40) NOT NULL,
    status VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    unidade_referencia VARCHAR(30),
    booking_id BIGINT REFERENCES gate_booking (id),
    order_id BIGINT REFERENCES gate_order (id),
    preadvice_id BIGINT REFERENCES gate_preadvice (id),
    stage_atual_id BIGINT REFERENCES gate_stage_config (id),
    trouble_ativo BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_gate_transaction_seq UNIQUE (truck_visit_id, sequencia),
    CONSTRAINT ck_gate_transaction_status CHECK (status IN ('PENDENTE', 'EM_PROCESSAMENTO', 'TROUBLE', 'CONCLUIDA', 'CANCELADA'))
);

CREATE TABLE gate_trouble_case (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES gate_transaction (id) ON DELETE CASCADE,
    codigo VARCHAR(60) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    severidade VARCHAR(20) NOT NULL DEFAULT 'MEDIA',
    status VARCHAR(20) NOT NULL DEFAULT 'ABERTO',
    aberto_por VARCHAR(120) NOT NULL,
    aberto_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    resolvido_por VARCHAR(120),
    resolvido_em TIMESTAMP WITHOUT TIME ZONE,
    resolucao VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gate_trouble_severidade CHECK (severidade IN ('BAIXA', 'MEDIA', 'ALTA', 'CRITICA')),
    CONSTRAINT ck_gate_trouble_status CHECK (status IN ('ABERTO', 'RESOLVIDO', 'CANCELADO'))
);

CREATE UNIQUE INDEX uk_gate_trouble_aberto
    ON gate_trouble_case (transaction_id)
    WHERE status = 'ABERTO';

CREATE TABLE gate_inspection (
    id BIGSERIAL PRIMARY KEY,
    transaction_id BIGINT NOT NULL REFERENCES gate_transaction (id) ON DELETE CASCADE,
    tipo VARCHAR(40) NOT NULL,
    resultado VARCHAR(30) NOT NULL,
    inspetor VARCHAR(120) NOT NULL,
    observacoes VARCHAR(500),
    executado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gate_inspection_resultado CHECK (resultado IN ('APROVADO', 'REPROVADO', 'COM_RESSALVA'))
);

CREATE TABLE gate_attachment (
    id BIGSERIAL PRIMARY KEY,
    truck_visit_id BIGINT REFERENCES truck_visit (id) ON DELETE CASCADE,
    transaction_id BIGINT REFERENCES gate_transaction (id) ON DELETE CASCADE,
    categoria VARCHAR(30) NOT NULL,
    nome_arquivo VARCHAR(255) NOT NULL,
    content_type VARCHAR(120),
    url_documento VARCHAR(500) NOT NULL,
    metadados JSONB NOT NULL DEFAULT '{}'::JSONB,
    criado_por VARCHAR(120) NOT NULL,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gate_attachment_vinculo CHECK (truck_visit_id IS NOT NULL OR transaction_id IS NOT NULL),
    CONSTRAINT ck_gate_attachment_categoria CHECK (categoria IN ('FOTOGRAFIA', 'DOCUMENTO', 'AVARIA', 'OCR', 'INSPECAO'))
);

CREATE TABLE gate_document (
    id BIGSERIAL PRIMARY KEY,
    truck_visit_id BIGINT NOT NULL REFERENCES truck_visit (id) ON DELETE CASCADE,
    transaction_id BIGINT REFERENCES gate_transaction (id) ON DELETE CASCADE,
    tipo VARCHAR(30) NOT NULL,
    numero VARCHAR(80) NOT NULL UNIQUE,
    status VARCHAR(20) NOT NULL DEFAULT 'EMITIDO',
    conteudo JSONB NOT NULL DEFAULT '{}'::JSONB,
    emitido_por VARCHAR(120) NOT NULL,
    emitido_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    reimpressoes INTEGER NOT NULL DEFAULT 0,
    ultima_reimpressao_em TIMESTAMP WITHOUT TIME ZONE,
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gate_document_tipo CHECK (tipo IN ('TICKET', 'EIR', 'COMPROVANTE_TRANSFERENCIA')),
    CONSTRAINT ck_gate_document_status CHECK (status IN ('EMITIDO', 'CANCELADO'))
);

CREATE TABLE gate_facility_transfer (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(60) NOT NULL UNIQUE,
    truck_visit_id BIGINT NOT NULL REFERENCES truck_visit (id),
    facility_origem_id BIGINT NOT NULL REFERENCES gate_facility (id),
    facility_destino_id BIGINT NOT NULL REFERENCES gate_facility (id),
    status VARCHAR(30) NOT NULL DEFAULT 'SOLICITADA',
    solicitado_por VARCHAR(120) NOT NULL,
    solicitado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    recebido_em TIMESTAMP WITHOUT TIME ZONE,
    observacoes VARCHAR(500),
    created_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT NOW(),
    CONSTRAINT ck_gate_transfer_facility CHECK (facility_origem_id <> facility_destino_id),
    CONSTRAINT ck_gate_transfer_status CHECK (status IN ('SOLICITADA', 'EM_TRANSITO', 'RECEBIDA', 'CANCELADA'))
);

CREATE INDEX idx_gate_lane_gate_status ON gate_lane (gate_id, status);
CREATE INDEX idx_gate_stage_gate_ordem ON gate_stage_config (gate_id, ordem);
CREATE INDEX idx_gate_booking_status ON gate_booking (status, validade_fim);
CREATE INDEX idx_gate_order_status ON gate_order (tipo, status, validade_fim);
CREATE INDEX idx_gate_preadvice_status ON gate_preadvice (tipo, status);
CREATE INDEX idx_truck_visit_operacao ON truck_visit (facility_id, gate_id, status, created_at);
CREATE INDEX idx_truck_visit_lane ON truck_visit (lane_id, status);
CREATE INDEX idx_gate_transaction_visit ON gate_transaction (truck_visit_id, sequencia);
CREATE INDEX idx_gate_transaction_status ON gate_transaction (status, trouble_ativo);
CREATE INDEX idx_gate_trouble_status ON gate_trouble_case (status, severidade, aberto_em);
CREATE INDEX idx_gate_inspection_transaction ON gate_inspection (transaction_id, executado_em);
CREATE INDEX idx_gate_attachment_visit ON gate_attachment (truck_visit_id, categoria);
CREATE INDEX idx_gate_document_visit ON gate_document (truck_visit_id, tipo);
CREATE INDEX idx_gate_transfer_status ON gate_facility_transfer (status, solicitado_em);

INSERT INTO gate_facility (codigo, nome)
VALUES ('TERMINAL-PRINCIPAL', 'Terminal Principal');

INSERT INTO gate_configuracao (facility_id, codigo, nome)
SELECT id, 'GATE-01', 'Gate Principal'
  FROM gate_facility
 WHERE codigo = 'TERMINAL-PRINCIPAL';

INSERT INTO gate_lane (gate_id, codigo, nome, direcao, capacidade_fila, ocr_habilitado, balanca_habilitada, inspecao_habilitada)
SELECT id, 'IN-01', 'Entrada 01', 'ENTRADA', 8, TRUE, TRUE, TRUE
  FROM gate_configuracao
 WHERE codigo = 'GATE-01';

INSERT INTO gate_lane (gate_id, codigo, nome, direcao, capacidade_fila, ocr_habilitado, balanca_habilitada, inspecao_habilitada)
SELECT id, 'OUT-01', 'Saída 01', 'SAIDA', 8, TRUE, FALSE, TRUE
  FROM gate_configuracao
 WHERE codigo = 'GATE-01';

INSERT INTO gate_stage_config (gate_id, codigo, nome, ordem, permite_trouble, finaliza_visita)
SELECT id, 'PRE_CHECK', 'Pré-check', 10, TRUE, FALSE FROM gate_configuracao WHERE codigo = 'GATE-01';
INSERT INTO gate_stage_config (gate_id, codigo, nome, ordem, permite_trouble, finaliza_visita)
SELECT id, 'OCR', 'OCR e identificação', 20, TRUE, FALSE FROM gate_configuracao WHERE codigo = 'GATE-01';
INSERT INTO gate_stage_config (gate_id, codigo, nome, ordem, permite_trouble, finaliza_visita)
SELECT id, 'BALANCA', 'Balança', 30, TRUE, FALSE FROM gate_configuracao WHERE codigo = 'GATE-01';
INSERT INTO gate_stage_config (gate_id, codigo, nome, ordem, permite_trouble, finaliza_visita)
SELECT id, 'INSPECAO', 'Inspeção', 40, TRUE, FALSE FROM gate_configuracao WHERE codigo = 'GATE-01';
INSERT INTO gate_stage_config (gate_id, codigo, nome, ordem, permite_trouble, finaliza_visita)
SELECT id, 'LIBERACAO', 'Liberação', 50, TRUE, FALSE FROM gate_configuracao WHERE codigo = 'GATE-01';
INSERT INTO gate_stage_config (gate_id, codigo, nome, ordem, permite_trouble, finaliza_visita)
SELECT id, 'CONCLUSAO', 'Conclusão', 60, FALSE, TRUE FROM gate_configuracao WHERE codigo = 'GATE-01';

INSERT INTO gate_stage_transition (origem_stage_id, destino_stage_id)
SELECT origem.id, destino.id
  FROM gate_stage_config origem
  JOIN gate_stage_config destino ON destino.gate_id = origem.gate_id AND destino.ordem = origem.ordem + 10
 WHERE origem.gate_id = (SELECT id FROM gate_configuracao WHERE codigo = 'GATE-01');

INSERT INTO gate_business_task (stage_id, codigo, nome, tipo, ordem, obrigatoria, configuracao)
SELECT id, 'VALIDAR_REFERENCIAS', 'Validar booking, ordem e pré-aviso', 'VALIDACAO', 10, TRUE,
       '{"aceitaBooking":true,"aceitaOrdens":["EDO","ERO","IDO"]}'::JSONB
  FROM gate_stage_config WHERE codigo = 'PRE_CHECK';
INSERT INTO gate_business_task (stage_id, codigo, nome, tipo, ordem, obrigatoria, configuracao)
SELECT id, 'CAPTURAR_OCR', 'Capturar placa e unidade por OCR', 'CAPTURA', 10, TRUE,
       '{"placa":true,"unidade":true}'::JSONB
  FROM gate_stage_config WHERE codigo = 'OCR';
INSERT INTO gate_business_task (stage_id, codigo, nome, tipo, ordem, obrigatoria, configuracao)
SELECT id, 'REGISTRAR_PESO', 'Registrar peso da balança', 'CAPTURA', 10, TRUE,
       '{"unidade":"KG"}'::JSONB
  FROM gate_stage_config WHERE codigo = 'BALANCA';
INSERT INTO gate_business_task (stage_id, codigo, nome, tipo, ordem, obrigatoria, configuracao)
SELECT id, 'EXECUTAR_INSPECAO', 'Executar inspeção operacional', 'DECISAO', 10, TRUE,
       '{"permiteRessalva":true}'::JSONB
  FROM gate_stage_config WHERE codigo = 'INSPECAO';
INSERT INTO gate_business_task (stage_id, codigo, nome, tipo, ordem, obrigatoria, configuracao)
SELECT id, 'EMITIR_DOCUMENTOS', 'Emitir ticket e EIR', 'IMPRESSAO', 10, TRUE,
       '{"documentos":["TICKET","EIR"]}'::JSONB
  FROM gate_stage_config WHERE codigo = 'LIBERACAO';