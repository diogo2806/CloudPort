CREATE TABLE IF NOT EXISTS control_room_dispositivo (
    id BIGSERIAL PRIMARY KEY,
    equipamento_id BIGINT NOT NULL REFERENCES equipamento_patio (id) ON DELETE CASCADE,
    identificador VARCHAR(80) NOT NULL,
    protocolo VARCHAR(30) NOT NULL,
    status_integracao VARCHAR(30) NOT NULL DEFAULT 'DESCONECTADO',
    firmware VARCHAR(80),
    endereco_rede VARCHAR(120),
    ultima_sequencia BIGINT,
    ultimo_heartbeat_em TIMESTAMP WITHOUT TIME ZONE,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_control_room_dispositivo_identificador UNIQUE (identificador),
    CONSTRAINT uk_control_room_dispositivo_equipamento UNIQUE (equipamento_id),
    CONSTRAINT ck_control_room_dispositivo_status CHECK (
        status_integracao IN ('CONECTADO', 'DESCONECTADO', 'DEGRADADO', 'ERRO')
    )
);

CREATE TABLE IF NOT EXISTS control_room_telemetria_historico (
    id BIGSERIAL PRIMARY KEY,
    equipamento_id BIGINT NOT NULL REFERENCES equipamento_patio (id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    coordenada_x DOUBLE PRECISION,
    coordenada_y DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    posicao_mais_proxima VARCHAR(80),
    distancia_posicao_centimetros INTEGER,
    dentro_da_posicao BOOLEAN,
    origem VARCHAR(80) NOT NULL,
    operador_vmt VARCHAR(120),
    status_vmt VARCHAR(40),
    work_instruction_atual_id BIGINT,
    sequencia BIGINT NOT NULL,
    capturado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    recebido_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_control_room_telemetria_sequencia UNIQUE (equipamento_id, sequencia),
    CONSTRAINT ck_control_room_telemetria_distancia CHECK (
        distancia_posicao_centimetros IS NULL OR distancia_posicao_centimetros >= 0
    )
);

CREATE TABLE IF NOT EXISTS control_room_comando (
    id BIGSERIAL PRIMARY KEY,
    equipamento_id BIGINT NOT NULL REFERENCES equipamento_patio (id) ON DELETE CASCADE,
    dispositivo_id BIGINT REFERENCES control_room_dispositivo (id) ON DELETE SET NULL,
    tipo VARCHAR(40) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PENDENTE',
    parametros_json TEXT,
    mensagem VARCHAR(500),
    solicitado_por VARCHAR(120) NOT NULL,
    correlation_id VARCHAR(100),
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    enviado_em TIMESTAMP WITHOUT TIME ZONE,
    confirmado_em TIMESTAMP WITHOUT TIME ZONE,
    retorno_dispositivo VARCHAR(1000),
    sequencia_dispositivo BIGINT,
    CONSTRAINT ck_control_room_comando_tipo CHECK (
        tipo IN (
            'DISPONIBILIZAR',
            'INDISPONIBILIZAR',
            'ENVIAR_MENSAGEM',
            'MOVER_PARA_POSICAO',
            'SINCRONIZAR_TELEMETRIA',
            'RESETAR_POSICAO'
        )
    ),
    CONSTRAINT ck_control_room_comando_status CHECK (
        status IN ('PENDENTE', 'ENVIADO', 'EXECUTADO', 'FALHOU', 'CANCELADO')
    )
);

CREATE TABLE IF NOT EXISTS control_room_indisponibilidade (
    id BIGSERIAL PRIMARY KEY,
    equipamento_id BIGINT NOT NULL REFERENCES equipamento_patio (id) ON DELETE CASCADE,
    motivo VARCHAR(120) NOT NULL,
    observacao VARCHAR(1000),
    inicio_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fim_em TIMESTAMP WITHOUT TIME ZONE,
    aberto_por VARCHAR(120) NOT NULL,
    encerrado_por VARCHAR(120),
    comando_origem_id BIGINT REFERENCES control_room_comando (id) ON DELETE SET NULL,
    CONSTRAINT ck_control_room_indisponibilidade_periodo CHECK (
        fim_em IS NULL OR fim_em >= inicio_em
    )
);

CREATE TABLE IF NOT EXISTS control_room_alarme (
    id BIGSERIAL PRIMARY KEY,
    equipamento_id BIGINT NOT NULL REFERENCES equipamento_patio (id) ON DELETE CASCADE,
    tipo VARCHAR(50) NOT NULL,
    severidade VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'ATIVO',
    mensagem VARCHAR(500) NOT NULL,
    origem VARCHAR(80) NOT NULL,
    detalhes_json TEXT,
    aberto_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reconhecido_em TIMESTAMP WITHOUT TIME ZONE,
    reconhecido_por VARCHAR(120),
    resolvido_em TIMESTAMP WITHOUT TIME ZONE,
    resolvido_por VARCHAR(120),
    CONSTRAINT ck_control_room_alarme_severidade CHECK (
        severidade IN ('BAIXA', 'MEDIA', 'ALTA', 'CRITICA')
    ),
    CONSTRAINT ck_control_room_alarme_status CHECK (
        status IN ('ATIVO', 'RECONHECIDO', 'RESOLVIDO')
    )
);

CREATE INDEX IF NOT EXISTS idx_control_room_telemetria_equipamento_data
    ON control_room_telemetria_historico (equipamento_id, capturado_em DESC);

CREATE INDEX IF NOT EXISTS idx_control_room_comando_equipamento_status
    ON control_room_comando (equipamento_id, status, criado_em DESC);

CREATE INDEX IF NOT EXISTS idx_control_room_comando_dispositivo_status
    ON control_room_comando (dispositivo_id, status, criado_em);

CREATE INDEX IF NOT EXISTS idx_control_room_indisponibilidade_equipamento
    ON control_room_indisponibilidade (equipamento_id, inicio_em DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_control_room_indisponibilidade_aberta
    ON control_room_indisponibilidade (equipamento_id)
    WHERE fim_em IS NULL;

CREATE INDEX IF NOT EXISTS idx_control_room_alarme_status_severidade
    ON control_room_alarme (status, severidade, aberto_em DESC);

CREATE UNIQUE INDEX IF NOT EXISTS uk_control_room_alarme_ativo_tipo
    ON control_room_alarme (equipamento_id, tipo)
    WHERE status IN ('ATIVO', 'RECONHECIDO');
