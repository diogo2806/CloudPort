CREATE TABLE configuracao_dispatch (
    id BIGSERIAL PRIMARY KEY,
    tipo_escopo VARCHAR(20) NOT NULL,
    valor_escopo VARCHAR(120) NOT NULL,
    tipo_equipamento VARCHAR(40) NOT NULL,
    versao BIGINT NOT NULL,
    status VARCHAR(20) NOT NULL,
    modo_dispatch VARCHAR(30) NOT NULL,
    peso_prioridade NUMERIC(12, 4) NOT NULL,
    peso_distancia NUMERIC(12, 4) NOT NULL,
    peso_atraso NUMERIC(12, 4) NOT NULL,
    peso_congestionamento NUMERIC(12, 4) NOT NULL,
    velocidade_media_kmh NUMERIC(10, 3) NOT NULL,
    tempo_coleta_segundos INTEGER NOT NULL,
    tempo_entrega_segundos INTEGER NOT NULL,
    tolerancia_telemetria_segundos INTEGER NOT NULL,
    capacidade_simultanea INTEGER NOT NULL,
    limite_regional_che INTEGER NOT NULL,
    selecionar_auxiliar BOOLEAN NOT NULL,
    permitir_override BOOLEAN NOT NULL,
    vigente_de TIMESTAMP NOT NULL,
    vigente_ate TIMESTAMP,
    motivo VARCHAR(500) NOT NULL,
    criado_por VARCHAR(120) NOT NULL,
    criado_em TIMESTAMP NOT NULL,
    ativado_em TIMESTAMP,
    substitui_configuracao_id BIGINT REFERENCES configuracao_dispatch(id),
    rollback_configuracao_id BIGINT REFERENCES configuracao_dispatch(id),
    CONSTRAINT uk_configuracao_dispatch_versao UNIQUE (
        tipo_escopo, valor_escopo, tipo_equipamento, versao
    ),
    CONSTRAINT ck_configuracao_dispatch_escopo CHECK (
        tipo_escopo IN ('TERMINAL', 'PATIO', 'BLOCO', 'POW', 'POOL', 'FILA')
    ),
    CONSTRAINT ck_configuracao_dispatch_status CHECK (
        status IN ('RASCUNHO', 'ATIVA', 'INATIVA')
    ),
    CONSTRAINT ck_configuracao_dispatch_modo CHECK (
        modo_dispatch IN ('MANUAL', 'SEMIAUTOMATICO', 'AUTOMATICO')
    ),
    CONSTRAINT ck_configuracao_dispatch_vigencia CHECK (
        vigente_ate IS NULL OR vigente_ate > vigente_de
    ),
    CONSTRAINT ck_configuracao_dispatch_valores CHECK (
        peso_prioridade >= 0
        AND peso_distancia >= 0
        AND peso_atraso >= 0
        AND peso_congestionamento >= 0
        AND velocidade_media_kmh > 0
        AND tempo_coleta_segundos >= 0
        AND tempo_entrega_segundos >= 0
        AND tolerancia_telemetria_segundos > 0
        AND capacidade_simultanea > 0
        AND limite_regional_che > 0
    )
);

CREATE UNIQUE INDEX uk_configuracao_dispatch_ativa
    ON configuracao_dispatch (tipo_escopo, valor_escopo, tipo_equipamento)
    WHERE status = 'ATIVA';

CREATE INDEX idx_configuracao_dispatch_resolucao
    ON configuracao_dispatch (
        tipo_equipamento, tipo_escopo, valor_escopo, status, vigente_de, vigente_ate
    );

CREATE TABLE historico_configuracao_dispatch (
    id BIGSERIAL PRIMARY KEY,
    configuracao_id BIGINT NOT NULL REFERENCES configuracao_dispatch(id),
    acao VARCHAR(30) NOT NULL,
    versao BIGINT NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    operador VARCHAR(120) NOT NULL,
    detalhes VARCHAR(2000),
    ocorrido_em TIMESTAMP NOT NULL,
    CONSTRAINT ck_historico_configuracao_dispatch_acao CHECK (
        acao IN ('CRIADA', 'ATIVADA', 'INATIVADA', 'ROLLBACK')
    )
);

CREATE INDEX idx_historico_configuracao_dispatch
    ON historico_configuracao_dispatch (configuracao_id, ocorrido_em DESC);

CREATE TABLE segmento_rota_dispatch (
    id BIGSERIAL PRIMARY KEY,
    origem VARCHAR(120) NOT NULL,
    destino VARCHAR(120) NOT NULL,
    distancia_metros NUMERIC(14, 3) NOT NULL,
    sentido VARCHAR(40),
    congestionamento_percentual NUMERIC(7, 3) NOT NULL DEFAULT 0,
    bloqueado BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_interdicao VARCHAR(500),
    limite_regional_che INTEGER,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    versao BIGINT NOT NULL DEFAULT 1,
    vigente_de TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    vigente_ate TIMESTAMP,
    atualizado_por VARCHAR(120) NOT NULL DEFAULT 'sistema',
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_segmento_rota_dispatch_versao UNIQUE (origem, destino, versao),
    CONSTRAINT ck_segmento_rota_dispatch_distancia CHECK (distancia_metros >= 0),
    CONSTRAINT ck_segmento_rota_dispatch_congestionamento CHECK (
        congestionamento_percentual BETWEEN 0 AND 100
    ),
    CONSTRAINT ck_segmento_rota_dispatch_vigencia CHECK (
        vigente_ate IS NULL OR vigente_ate > vigente_de
    )
);

CREATE INDEX idx_segmento_rota_dispatch_consulta
    ON segmento_rota_dispatch (origem, destino, ativo, vigente_de, vigente_ate, versao DESC);

CREATE TABLE decisao_dispatch (
    id BIGSERIAL PRIMARY KEY,
    chave_idempotencia VARCHAR(120) NOT NULL UNIQUE,
    work_queue_id BIGINT NOT NULL REFERENCES work_queue_patio(id),
    ordem_trabalho_patio_id BIGINT NOT NULL REFERENCES ordem_trabalho_patio(id),
    equipamento_patio_id BIGINT NOT NULL REFERENCES equipamento_patio(id),
    configuracao_dispatch_id BIGINT NOT NULL REFERENCES configuracao_dispatch(id),
    versao_configuracao BIGINT NOT NULL,
    modo_dispatch VARCHAR(30) NOT NULL,
    score NUMERIC(18, 6) NOT NULL,
    memoria_calculo VARCHAR(4000) NOT NULL,
    origem_rota VARCHAR(120),
    destino_rota VARCHAR(120),
    distancia_metros NUMERIC(14, 3),
    congestionamento_percentual NUMERIC(7, 3),
    eta_segundos INTEGER,
    telemetria_recebida_em TIMESTAMP,
    telemetria_atrasada BOOLEAN NOT NULL,
    rota_bloqueada BOOLEAN NOT NULL,
    status VARCHAR(20) NOT NULL,
    motivo VARCHAR(500) NOT NULL,
    operador VARCHAR(120) NOT NULL,
    correlation_id VARCHAR(100),
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    CONSTRAINT ck_decisao_dispatch_modo CHECK (
        modo_dispatch IN ('MANUAL', 'SEMIAUTOMATICO', 'AUTOMATICO')
    ),
    CONSTRAINT ck_decisao_dispatch_status CHECK (
        status IN ('RECOMENDADA', 'ATRIBUIDA', 'REJEITADA', 'CANCELADA', 'CONCLUIDA', 'FALHA')
    )
);

CREATE INDEX idx_decisao_dispatch_fila
    ON decisao_dispatch (work_queue_id, criado_em DESC);

CREATE INDEX idx_decisao_dispatch_equipamento
    ON decisao_dispatch (equipamento_patio_id, status, criado_em DESC);

CREATE INDEX idx_decisao_dispatch_instrucao
    ON decisao_dispatch (ordem_trabalho_patio_id, criado_em DESC);

CREATE TABLE etapa_work_instruction (
    id BIGSERIAL PRIMARY KEY,
    ordem_trabalho_patio_id BIGINT NOT NULL REFERENCES ordem_trabalho_patio(id),
    decisao_dispatch_id BIGINT REFERENCES decisao_dispatch(id),
    tipo_etapa VARCHAR(40) NOT NULL,
    ordem_etapa INTEGER NOT NULL,
    status VARCHAR(20) NOT NULL,
    iniciado_em TIMESTAMP,
    concluido_em TIMESTAMP,
    operador VARCHAR(120),
    evidencia VARCHAR(500),
    chave_idempotencia VARCHAR(120),
    atualizado_em TIMESTAMP NOT NULL,
    CONSTRAINT uk_etapa_work_instruction_tipo UNIQUE (
        ordem_trabalho_patio_id, tipo_etapa
    ),
    CONSTRAINT uk_etapa_work_instruction_ordem UNIQUE (
        ordem_trabalho_patio_id, ordem_etapa
    ),
    CONSTRAINT ck_etapa_work_instruction_tipo CHECK (
        tipo_etapa IN (
            'DESLOCAMENTO_ORIGEM', 'CHEGADA_ORIGEM', 'COLETA',
            'TRANSPORTE', 'ENTREGA', 'CONFIRMACAO_FISICA'
        )
    ),
    CONSTRAINT ck_etapa_work_instruction_status CHECK (
        status IN ('PENDENTE', 'EM_EXECUCAO', 'CONCLUIDA', 'FALHA', 'IGNORADA')
    ),
    CONSTRAINT ck_etapa_work_instruction_ordem CHECK (ordem_etapa BETWEEN 1 AND 6)
);

CREATE UNIQUE INDEX uk_etapa_work_instruction_idempotencia
    ON etapa_work_instruction (chave_idempotencia)
    WHERE chave_idempotencia IS NOT NULL;

CREATE INDEX idx_etapa_work_instruction_ciclo
    ON etapa_work_instruction (ordem_trabalho_patio_id, ordem_etapa);

CREATE TABLE reserva_equipamento_auxiliar_dispatch (
    id BIGSERIAL PRIMARY KEY,
    decisao_dispatch_id BIGINT NOT NULL REFERENCES decisao_dispatch(id),
    ordem_trabalho_patio_id BIGINT NOT NULL REFERENCES ordem_trabalho_patio(id),
    unidade_inventario_id BIGINT NOT NULL REFERENCES unidade_inventario(id),
    tipo_auxiliar VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    pool_operacional VARCHAR(80),
    armador VARCHAR(120),
    origem VARCHAR(120),
    destino VARCHAR(120),
    motivo_selecao VARCHAR(500) NOT NULL,
    reservado_por VARCHAR(120) NOT NULL,
    reservado_em TIMESTAMP NOT NULL,
    associado_em TIMESTAMP,
    devolvido_em TIMESTAMP,
    CONSTRAINT ck_reserva_auxiliar_tipo CHECK (
        tipo_auxiliar IN ('CHASSI', 'BOMB_CART', 'CASSETTE', 'ACESSORIO')
    ),
    CONSTRAINT ck_reserva_auxiliar_status CHECK (
        status IN ('RESERVADO', 'ASSOCIADO', 'DEVOLVIDO', 'CANCELADO')
    )
);

CREATE UNIQUE INDEX uk_reserva_auxiliar_ativa
    ON reserva_equipamento_auxiliar_dispatch (unidade_inventario_id)
    WHERE status IN ('RESERVADO', 'ASSOCIADO');

CREATE INDEX idx_reserva_auxiliar_instrucao
    ON reserva_equipamento_auxiliar_dispatch (ordem_trabalho_patio_id, reservado_em DESC);

CREATE TABLE movimento_equipamento_auxiliar_dispatch (
    id BIGSERIAL PRIMARY KEY,
    reserva_id BIGINT NOT NULL REFERENCES reserva_equipamento_auxiliar_dispatch(id),
    tipo_movimento VARCHAR(30) NOT NULL,
    origem VARCHAR(120),
    destino VARCHAR(120),
    operador VARCHAR(120) NOT NULL,
    ocorrido_em TIMESTAMP NOT NULL,
    detalhes VARCHAR(1000),
    CONSTRAINT ck_movimento_auxiliar_tipo CHECK (
        tipo_movimento IN ('COLETA', 'ASSOCIACAO', 'TRANSPORTE', 'DESASSOCIACAO', 'DEVOLUCAO')
    )
);

CREATE TABLE gatilho_dispatch_processado (
    id BIGSERIAL PRIMARY KEY,
    ordem_trabalho_patio_id BIGINT NOT NULL REFERENCES ordem_trabalho_patio(id),
    evento VARCHAR(30) NOT NULL,
    assinatura VARCHAR(160) NOT NULL UNIQUE,
    processado_em TIMESTAMP NOT NULL,
    resultado VARCHAR(1000),
    CONSTRAINT ck_gatilho_dispatch_evento CHECK (
        evento IN ('VMT_ACEITE', 'VMT_INICIO', 'VMT_FALHA', 'VMT_CONCLUSAO')
    )
);

INSERT INTO configuracao_dispatch (
    tipo_escopo, valor_escopo, tipo_equipamento, versao, status, modo_dispatch,
    peso_prioridade, peso_distancia, peso_atraso, peso_congestionamento,
    velocidade_media_kmh, tempo_coleta_segundos, tempo_entrega_segundos,
    tolerancia_telemetria_segundos, capacidade_simultanea, limite_regional_che,
    selecionar_auxiliar, permitir_override, vigente_de, vigente_ate,
    motivo, criado_por, criado_em, ativado_em
)
SELECT
    'TERMINAL', 'PADRAO', tipo_equipamento, 1, 'ATIVA', modo_dispatch,
    peso_prioridade, peso_distancia, peso_atraso, peso_congestionamento,
    velocidade_media_kmh, tempo_coleta_segundos, tempo_entrega_segundos,
    120, capacidade_simultanea, limite_regional_che,
    selecionar_auxiliar, TRUE, CURRENT_TIMESTAMP, NULL,
    'Perfil operacional inicial versionado do scheduler de dispatch.',
    'migration-v220', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM (VALUES
    ('RTG', 'SEMIAUTOMATICO', 12.0, 0.8, 3.5, 2.0, 18.0, 90, 90, 1, 6, FALSE),
    ('RMG', 'SEMIAUTOMATICO', 12.0, 0.7, 3.5, 1.5, 20.0, 75, 75, 1, 6, FALSE),
    ('ASC', 'AUTOMATICO', 13.0, 0.6, 4.0, 2.0, 22.0, 60, 60, 2, 8, FALSE),
    ('REACH_STACKER', 'SEMIAUTOMATICO', 11.0, 1.0, 3.0, 2.5, 16.0, 120, 120, 1, 5, FALSE),
    ('TRATOR_PORTUARIO', 'AUTOMATICO', 10.0, 1.5, 4.0, 4.0, 28.0, 60, 60, 1, 12, TRUE),
    ('STRADDLE_CARRIER', 'AUTOMATICO', 12.0, 1.2, 4.0, 3.0, 25.0, 70, 70, 1, 10, FALSE),
    ('GUINDASTE_SHIP_TO_SHORE', 'SEMIAUTOMATICO', 15.0, 0.3, 5.0, 1.0, 10.0, 150, 150, 1, 4, TRUE),
    ('EQUIPAMENTO_FERROVIARIO', 'SEMIAUTOMATICO', 14.0, 0.9, 4.5, 2.0, 15.0, 120, 120, 1, 6, FALSE)
) AS perfil(
    tipo_equipamento, modo_dispatch, peso_prioridade, peso_distancia,
    peso_atraso, peso_congestionamento, velocidade_media_kmh,
    tempo_coleta_segundos, tempo_entrega_segundos,
    capacidade_simultanea, limite_regional_che, selecionar_auxiliar
);

INSERT INTO historico_configuracao_dispatch (
    configuracao_id, acao, versao, motivo, operador, detalhes, ocorrido_em
)
SELECT
    id, 'ATIVADA', versao, motivo, criado_por,
    'Configuração inicial criada e ativada pela migration V220.', criado_em
FROM configuracao_dispatch
WHERE criado_por = 'migration-v220';
