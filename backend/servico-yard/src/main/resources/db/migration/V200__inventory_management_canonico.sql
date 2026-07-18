-- Inventory Management canônico: unidade, equipamento e inventário físico.

CREATE TABLE IF NOT EXISTS tipo_equipamento_inventario (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    descricao VARCHAR(120) NOT NULL,
    categoria VARCHAR(30) NOT NULL,
    codigo_iso VARCHAR(10),
    comprimento_mm INTEGER,
    largura_mm INTEGER,
    altura_mm INTEGER,
    tara_kg NUMERIC(12,3),
    capacidade_kg NUMERIC(12,3),
    refrigerado BOOLEAN NOT NULL DEFAULT FALSE,
    grupo_equivalencia VARCHAR(60),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_tipo_equipamento_categoria
        CHECK (categoria IN ('CONTEINER', 'CHASSI', 'CARRETA', 'ACESSORIO')),
    CONSTRAINT ck_tipo_equipamento_dimensoes
        CHECK ((comprimento_mm IS NULL OR comprimento_mm > 0)
           AND (largura_mm IS NULL OR largura_mm > 0)
           AND (altura_mm IS NULL OR altura_mm > 0)),
    CONSTRAINT ck_tipo_equipamento_pesos
        CHECK ((tara_kg IS NULL OR tara_kg >= 0)
           AND (capacidade_kg IS NULL OR capacidade_kg >= 0))
);

CREATE INDEX IF NOT EXISTS idx_tipo_equipamento_equivalencia
    ON tipo_equipamento_inventario (grupo_equivalencia, ativo);

CREATE TABLE IF NOT EXISTS prefixo_equipamento_inventario (
    id BIGSERIAL PRIMARY KEY,
    prefixo VARCHAR(12) NOT NULL UNIQUE,
    proprietario VARCHAR(120),
    categoria VARCHAR(30) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_prefixo_equipamento_categoria
        CHECK (categoria IN ('CONTEINER', 'CHASSI', 'CARRETA', 'ACESSORIO'))
);

CREATE TABLE IF NOT EXISTS unidade_inventario (
    id BIGSERIAL PRIMARY KEY,
    identificacao VARCHAR(40) NOT NULL UNIQUE,
    prefixo VARCHAR(12),
    tipo_equipamento_id BIGINT NOT NULL REFERENCES tipo_equipamento_inventario(id),
    categoria VARCHAR(30) NOT NULL,
    estado_unidade VARCHAR(30) NOT NULL,
    condicao_equipamento VARCHAR(30) NOT NULL,
    status_manutencao VARCHAR(30) NOT NULL,
    proprietario VARCHAR(120),
    operador VARCHAR(120),
    posicao_atual VARCHAR(120),
    posicao_planejada VARCHAR(120),
    peso_bruto_kg NUMERIC(14,3),
    observacoes VARCHAR(1000),
    versao BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_unidade_categoria
        CHECK (categoria IN ('CONTEINER', 'CHASSI', 'CARRETA', 'ACESSORIO')),
    CONSTRAINT ck_unidade_estado
        CHECK (estado_unidade IN ('PRE_AVISADA', 'ATIVA', 'NO_PATIO', 'EM_OPERACAO',
                                  'EM_TRANSITO', 'EMBARCADA', 'DESEMBARCADA', 'LIBERADA',
                                  'DESPACHADA', 'INATIVA', 'APOSENTADA')),
    CONSTRAINT ck_unidade_condicao
        CHECK (condicao_equipamento IN ('OPERACIONAL', 'AVARIADO', 'INOPERANTE',
                                        'EM_INSPECAO', 'EM_REPARO', 'AGUARDANDO_PECA')),
    CONSTRAINT ck_unidade_manutencao
        CHECK (status_manutencao IN ('NAO_REQUERIDA', 'ABERTA', 'EM_EXECUCAO',
                                     'SUSPENSA', 'CONCLUIDA')),
    CONSTRAINT ck_unidade_peso CHECK (peso_bruto_kg IS NULL OR peso_bruto_kg >= 0)
);

CREATE INDEX IF NOT EXISTS idx_unidade_inventario_categoria_estado
    ON unidade_inventario (categoria, estado_unidade);
CREATE INDEX IF NOT EXISTS idx_unidade_inventario_prefixo
    ON unidade_inventario (prefixo);
CREATE INDEX IF NOT EXISTS idx_unidade_inventario_posicao
    ON unidade_inventario (posicao_atual);
CREATE INDEX IF NOT EXISTS idx_unidade_inventario_owner_operator
    ON unidade_inventario (proprietario, operador);

CREATE TABLE IF NOT EXISTS unidade_lacre (
    unidade_id BIGINT NOT NULL REFERENCES unidade_inventario(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    numero VARCHAR(60) NOT NULL,
    tipo VARCHAR(40),
    status VARCHAR(30) NOT NULL,
    anexado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    removido_em TIMESTAMP WITHOUT TIME ZONE,
    responsavel VARCHAR(120),
    PRIMARY KEY (unidade_id, ordem)
);
CREATE INDEX IF NOT EXISTS idx_unidade_lacre_numero ON unidade_lacre (numero);

CREATE TABLE IF NOT EXISTS unidade_documento (
    unidade_id BIGINT NOT NULL REFERENCES unidade_inventario(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    tipo_documento VARCHAR(60) NOT NULL,
    numero_documento VARCHAR(100),
    uri_documento VARCHAR(500),
    checksum VARCHAR(128),
    status_documento VARCHAR(30) NOT NULL,
    valido_ate DATE,
    registrado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (unidade_id, ordem),
    CONSTRAINT ck_unidade_documento_status
        CHECK (status_documento IN ('PENDENTE', 'VALIDO', 'EXPIRADO', 'CANCELADO'))
);

CREATE TABLE IF NOT EXISTS unidade_avaria (
    unidade_id BIGINT NOT NULL REFERENCES unidade_inventario(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    componente VARCHAR(80) NOT NULL,
    tipo_avaria VARCHAR(80) NOT NULL,
    severidade VARCHAR(30) NOT NULL,
    status_avaria VARCHAR(30) NOT NULL,
    descricao VARCHAR(500),
    detectada_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    reparada_em TIMESTAMP WITHOUT TIME ZONE,
    responsavel VARCHAR(120),
    PRIMARY KEY (unidade_id, ordem),
    CONSTRAINT ck_unidade_avaria_status
        CHECK (status_avaria IN ('ABERTA', 'EM_REPARO', 'REPARADA', 'ACEITA'))
);
CREATE INDEX IF NOT EXISTS idx_unidade_avaria_status
    ON unidade_avaria (status_avaria, detectada_em DESC);

CREATE TABLE IF NOT EXISTS unidade_restricao (
    unidade_id BIGINT NOT NULL REFERENCES unidade_inventario(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    tipo_restricao VARCHAR(20) NOT NULL,
    codigo_restricao VARCHAR(60) NOT NULL,
    descricao VARCHAR(500),
    autoridade VARCHAR(120),
    ativa BOOLEAN NOT NULL,
    valido_de TIMESTAMP WITHOUT TIME ZONE,
    valido_ate TIMESTAMP WITHOUT TIME ZONE,
    registrado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (unidade_id, ordem),
    CONSTRAINT ck_unidade_restricao_tipo CHECK (tipo_restricao IN ('HOLD', 'PERMISSION')),
    CONSTRAINT ck_unidade_restricao_validade
        CHECK (valido_de IS NULL OR valido_ate IS NULL OR valido_ate >= valido_de)
);
CREATE INDEX IF NOT EXISTS idx_unidade_restricao_ativa
    ON unidade_restricao (tipo_restricao, ativa, valido_ate);

CREATE TABLE IF NOT EXISTS unidade_manutencao (
    unidade_id BIGINT NOT NULL REFERENCES unidade_inventario(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    ordem_servico VARCHAR(60) NOT NULL,
    tipo_servico VARCHAR(100) NOT NULL,
    fornecedor VARCHAR(120),
    status_manutencao_registro VARCHAR(30) NOT NULL,
    aberta_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    concluida_em TIMESTAMP WITHOUT TIME ZONE,
    observacoes VARCHAR(500),
    PRIMARY KEY (unidade_id, ordem),
    CONSTRAINT ck_unidade_manutencao_status
        CHECK (status_manutencao_registro IN ('NAO_REQUERIDA', 'ABERTA', 'EM_EXECUCAO',
                                              'SUSPENSA', 'CONCLUIDA'))
);
CREATE INDEX IF NOT EXISTS idx_unidade_manutencao_status
    ON unidade_manutencao (status_manutencao_registro, aberta_em DESC);

CREATE TABLE IF NOT EXISTS unidade_historico_atributo (
    unidade_id BIGINT NOT NULL REFERENCES unidade_inventario(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    atributo VARCHAR(80) NOT NULL,
    valor_anterior VARCHAR(1000),
    valor_atual VARCHAR(1000),
    origem VARCHAR(80),
    responsavel VARCHAR(120),
    alterado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    PRIMARY KEY (unidade_id, ordem)
);
CREATE INDEX IF NOT EXISTS idx_unidade_historico_data
    ON unidade_historico_atributo (unidade_id, alterado_em DESC);

CREATE TABLE IF NOT EXISTS unidade_reefer_registro (
    unidade_id BIGINT NOT NULL REFERENCES unidade_inventario(id) ON DELETE CASCADE,
    ordem INTEGER NOT NULL,
    setpoint_c NUMERIC(7,3),
    temperatura_supply_c NUMERIC(7,3),
    temperatura_return_c NUMERIC(7,3),
    umidade_percentual NUMERIC(6,3),
    ventilacao_m3h NUMERIC(9,3),
    ligado BOOLEAN NOT NULL,
    alarme VARCHAR(255),
    lido_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    responsavel VARCHAR(120),
    PRIMARY KEY (unidade_id, ordem),
    CONSTRAINT ck_unidade_reefer_umidade
        CHECK (umidade_percentual IS NULL OR (umidade_percentual >= 0 AND umidade_percentual <= 100)),
    CONSTRAINT ck_unidade_reefer_ventilacao
        CHECK (ventilacao_m3h IS NULL OR ventilacao_m3h >= 0)
);
CREATE INDEX IF NOT EXISTS idx_unidade_reefer_leitura
    ON unidade_reefer_registro (unidade_id, lido_em DESC);

CREATE TABLE IF NOT EXISTS vinculo_equipamento (
    id BIGSERIAL PRIMARY KEY,
    unidade_principal_id BIGINT NOT NULL REFERENCES unidade_inventario(id),
    unidade_relacionada_id BIGINT NOT NULL REFERENCES unidade_inventario(id),
    papel VARCHAR(30) NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    montado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    desmontado_em TIMESTAMP WITHOUT TIME ZONE,
    responsavel_montagem VARCHAR(120),
    responsavel_desmontagem VARCHAR(120),
    observacoes VARCHAR(500),
    CONSTRAINT ck_vinculo_unidades_distintas CHECK (unidade_principal_id <> unidade_relacionada_id),
    CONSTRAINT ck_vinculo_papel
        CHECK (papel IN ('PRIMARIO', 'TRANSPORTE', 'PAYLOAD', 'ACESSORIO', 'ACESSORIO_NO_CHASSI'))
);
CREATE UNIQUE INDEX IF NOT EXISTS uk_vinculo_equipamento_ativo
    ON vinculo_equipamento (
        LEAST(unidade_principal_id, unidade_relacionada_id),
        GREATEST(unidade_principal_id, unidade_relacionada_id)
    ) WHERE ativo;
CREATE INDEX IF NOT EXISTS idx_vinculo_equipamento_principal
    ON vinculo_equipamento (unidade_principal_id, ativo);
CREATE INDEX IF NOT EXISTS idx_vinculo_equipamento_relacionada
    ON vinculo_equipamento (unidade_relacionada_id, ativo);

CREATE TABLE IF NOT EXISTS contagem_inventario_fisico (
    id BIGSERIAL PRIMARY KEY,
    lote VARCHAR(80) NOT NULL,
    unidade_id BIGINT REFERENCES unidade_inventario(id) ON DELETE SET NULL,
    identificacao_lida VARCHAR(40) NOT NULL,
    posicao_esperada VARCHAR(120),
    posicao_lida VARCHAR(120),
    status_contagem VARCHAR(30) NOT NULL,
    tipo_divergencia VARCHAR(40),
    observacoes VARCHAR(500),
    responsavel VARCHAR(120),
    registrado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    resolvido_em TIMESTAMP WITHOUT TIME ZONE,
    resolvido_por VARCHAR(120),
    CONSTRAINT ck_contagem_status
        CHECK (status_contagem IN ('CONFERENTE', 'DIVERGENTE', 'NAO_LOCALIZADA',
                                   'NAO_PREVISTA', 'RESOLVIDA')),
    CONSTRAINT ck_contagem_divergencia
        CHECK (tipo_divergencia IS NULL OR tipo_divergencia IN ('POSICAO', 'UNIDADE_NAO_LOCALIZADA',
              'UNIDADE_NAO_PREVISTA', 'ATRIBUTO', 'LACRE', 'CONDICAO'))
);
CREATE INDEX IF NOT EXISTS idx_contagem_inventario_lote
    ON contagem_inventario_fisico (lote, registrado_em);
CREATE INDEX IF NOT EXISTS idx_contagem_inventario_divergencia
    ON contagem_inventario_fisico (status_contagem, registrado_em DESC);

INSERT INTO tipo_equipamento_inventario
    (codigo, descricao, categoria, codigo_iso, comprimento_mm, largura_mm, altura_mm,
     tara_kg, capacidade_kg, refrigerado, grupo_equivalencia, ativo)
VALUES
    ('CTR-UNKNOWN', 'Contêiner sem tipo ISO informado', 'CONTEINER', NULL, NULL, NULL, NULL,
     NULL, NULL, FALSE, NULL, TRUE),
    ('CTR-20GP', 'Contêiner dry 20 pés', 'CONTEINER', '22G1', 6058, 2438, 2591,
     2200, 28280, FALSE, 'DRY-20', TRUE),
    ('CTR-40GP', 'Contêiner dry 40 pés', 'CONTEINER', '42G1', 12192, 2438, 2591,
     3800, 26700, FALSE, 'DRY-40', TRUE),
    ('CTR-40HC', 'Contêiner high cube 40 pés', 'CONTEINER', '45G1', 12192, 2438, 2896,
     3900, 26600, FALSE, 'DRY-40', TRUE),
    ('CTR-40RF', 'Contêiner reefer 40 pés', 'CONTEINER', '42R1', 12192, 2438, 2591,
     4500, 29500, TRUE, 'REEFER-40', TRUE),
    ('CHS-20', 'Chassi para contêiner 20 pés', 'CHASSI', NULL, 6058, 2438, NULL,
     NULL, NULL, FALSE, 'CHASSI-20', TRUE),
    ('CHS-40', 'Chassi para contêiner 40 pés', 'CHASSI', NULL, 12192, 2438, NULL,
     NULL, NULL, FALSE, 'CHASSI-40', TRUE),
    ('TRL-GENERIC', 'Carreta portuária genérica', 'CARRETA', NULL, NULL, NULL, NULL,
     NULL, NULL, FALSE, 'CARRETA', TRUE),
    ('ACC-GENSET', 'Gerador para contêiner reefer', 'ACESSORIO', NULL, NULL, NULL, NULL,
     NULL, NULL, FALSE, 'GENSET', TRUE)
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO unidade_inventario
    (identificacao, prefixo, tipo_equipamento_id, categoria, estado_unidade,
     condicao_equipamento, status_manutencao, posicao_atual, posicao_planejada,
     peso_bruto_kg, observacoes, criado_em, atualizado_em)
SELECT
    cp.codigo,
    CASE WHEN substring(upper(cp.codigo) FROM '^[A-Z]{2,4}') <> ''
         THEN substring(upper(cp.codigo) FROM '^[A-Z]{2,4}') ELSE NULL END,
    te.id,
    'CONTEINER',
    CASE cp.status_conteiner
        WHEN 'LIBERADO' THEN 'LIBERADA'
        WHEN 'DESPACHADO' THEN 'DESPACHADA'
        ELSE 'NO_PATIO'
    END,
    CASE cp.status_conteiner WHEN 'DANIFICADO' THEN 'AVARIADO' ELSE 'OPERACIONAL' END,
    'NAO_REQUERIDA',
    CASE WHEN pp.id IS NULL THEN NULL
         ELSE pp.linha || '-' || pp.coluna || '-' || pp.camada_operacional END,
    CASE WHEN pp.id IS NULL THEN NULL
         ELSE pp.linha || '-' || pp.coluna || '-' || pp.camada_operacional END,
    CASE WHEN cp.peso_toneladas IS NULL THEN NULL ELSE cp.peso_toneladas * 1000 END,
    cp.restricoes,
    COALESCE(cp.atualizado_em, CURRENT_TIMESTAMP),
    COALESCE(cp.atualizado_em, CURRENT_TIMESTAMP)
FROM conteiner_patio cp
JOIN tipo_equipamento_inventario te ON te.codigo = 'CTR-UNKNOWN'
LEFT JOIN posicao_patio pp ON pp.id = cp.posicao_id
ON CONFLICT (identificacao) DO NOTHING;

INSERT INTO prefixo_equipamento_inventario (prefixo, categoria, ativo)
SELECT DISTINCT ui.prefixo, ui.categoria, TRUE
FROM unidade_inventario ui
WHERE ui.prefixo IS NOT NULL
ON CONFLICT (prefixo) DO NOTHING;

INSERT INTO unidade_historico_atributo
    (unidade_id, ordem, atributo, valor_anterior, valor_atual, origem, responsavel, alterado_em)
SELECT ui.id, 0, 'IMPORTACAO_LEGADO', NULL, ui.estado_unidade,
       'MIGRACAO_V200', 'flyway', ui.criado_em
FROM unidade_inventario ui
WHERE NOT EXISTS (
    SELECT 1 FROM unidade_historico_atributo h
    WHERE h.unidade_id = ui.id AND h.atributo = 'IMPORTACAO_LEGADO'
);
