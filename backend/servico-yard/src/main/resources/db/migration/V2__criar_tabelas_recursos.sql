CREATE TABLE IF NOT EXISTS berco_portuario (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(30) NOT NULL UNIQUE,
    nome VARCHAR(120) NOT NULL,
    comprimento_metros INTEGER NOT NULL,
    calado_metros NUMERIC(6,2) NOT NULL,
    guinches_permanentes INTEGER NOT NULL,
    capacidade_toneladas_dia INTEGER NOT NULL,
    voltagem VARCHAR(40) NOT NULL,
    agua_potavel BOOLEAN NOT NULL DEFAULT FALSE,
    energia_generica BOOLEAN NOT NULL DEFAULT FALSE,
    iluminacao_noturna BOOLEAN NOT NULL DEFAULT FALSE,
    sistema_seguranca BOOLEAN NOT NULL DEFAULT FALSE,
    cobertura BOOLEAN NOT NULL DEFAULT FALSE,
    compat_container BOOLEAN NOT NULL DEFAULT FALSE,
    compat_breakbulk BOOLEAN NOT NULL DEFAULT FALSE,
    compat_roro BOOLEAN NOT NULL DEFAULT FALSE,
    compat_carga_geral BOOLEAN NOT NULL DEFAULT FALSE,
    compat_reefer BOOLEAN NOT NULL DEFAULT FALSE,
    compat_perigosa BOOLEAN NOT NULL DEFAULT FALSE,
    compat_granel BOOLEAN NOT NULL DEFAULT FALSE,
    zona_primaria VARCHAR(40) NOT NULL,
    zona_secundaria VARCHAR(40),
    distancia_zona_metros INTEGER NOT NULL DEFAULT 0,
    tempo_transporte_minutos INTEGER NOT NULL DEFAULT 0,
    dias_operacao VARCHAR(80) NOT NULL,
    ultima_manutencao DATE,
    proxima_manutencao DATE,
    status VARCHAR(30) NOT NULL,
    observacoes VARCHAR(250)
);

CREATE TABLE IF NOT EXISTS zona_armazenagem (
    id BIGSERIAL PRIMARY KEY,
    codigo VARCHAR(40) NOT NULL UNIQUE,
    nome VARCHAR(120) NOT NULL,
    capacidade_total INTEGER NOT NULL,
    ocupacao_atual INTEGER NOT NULL,
    bloqueada BOOLEAN NOT NULL DEFAULT FALSE,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    observacao VARCHAR(200)
);

CREATE TABLE IF NOT EXISTS reserva_berco (
    id BIGSERIAL PRIMARY KEY,
    berco_id BIGINT NOT NULL REFERENCES berco_portuario(id),
    navio_codigo VARCHAR(50) NOT NULL,
    navio_nome VARCHAR(120) NOT NULL,
    chegada_prevista TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    saida_prevista TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    comprimento_navio INTEGER NOT NULL,
    calado_navio NUMERIC(6,2) NOT NULL,
    guinches_requeridos INTEGER NOT NULL,
    tipo_carga VARCHAR(40) NOT NULL,
    zona_armazenagem VARCHAR(40) NOT NULL,
    tipo_reserva VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    score INTEGER NOT NULL DEFAULT 0,
    motivo VARCHAR(250) NOT NULL,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS equipamento_berco (
    id BIGSERIAL PRIMARY KEY,
    identificador VARCHAR(30) NOT NULL UNIQUE,
    tipo VARCHAR(50) NOT NULL,
    berco_codigo VARCHAR(30) NOT NULL,
    status VARCHAR(30) NOT NULL,
    ultima_verificacao TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_reserva_berco_janela ON reserva_berco (berco_id, chegada_prevista, saida_prevista);
CREATE INDEX IF NOT EXISTS idx_reserva_berco_datas ON reserva_berco (chegada_prevista, saida_prevista);

INSERT INTO berco_portuario (
    codigo, nome, comprimento_metros, calado_metros, guinches_permanentes, capacidade_toneladas_dia,
    voltagem, agua_potavel, energia_generica, iluminacao_noturna, sistema_seguranca, cobertura,
    compat_container, compat_breakbulk, compat_roro, compat_carga_geral, compat_reefer, compat_perigosa,
    compat_granel, zona_primaria, zona_secundaria, distancia_zona_metros, tempo_transporte_minutos,
    dias_operacao, ultima_manutencao, proxima_manutencao, status, observacoes
) VALUES
    ('BERCO_001', 'Berco Principal Sul', 400, 14.50, 2, 85000, '440V trifase', TRUE, TRUE, TRUE, TRUE, FALSE,
     TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, FALSE, 'ZONA_A', 'ZONA_B', 240, 10, 'SEG-DOM 24H',
     DATE '2026-05-15', DATE '2026-07-15', 'OPERACIONAL', 'Berco prioritario para navios de grande porte'),
    ('BERCO_002', 'Berco Secundario Norte', 280, 13.00, 2, 62000, '440V trifase', TRUE, TRUE, TRUE, TRUE, FALSE,
     TRUE, TRUE, FALSE, TRUE, TRUE, FALSE, FALSE, 'ZONA_A', 'ZONA_C', 180, 8, 'SEG-SAB 24H',
     DATE '2026-04-22', DATE '2026-06-20', 'OPERACIONAL', 'Mais usado para janelas curtas'),
    ('BERCO_003', 'Berco Sul Reefer', 350, 14.20, 3, 78000, '440V trifase', TRUE, TRUE, TRUE, TRUE, TRUE,
     TRUE, FALSE, TRUE, TRUE, TRUE, TRUE, FALSE, 'ZONA_B', 'ZONA_C', 120, 6, 'SEG-DOM 24H',
     DATE '2026-05-28', DATE '2026-06-12', 'OPERACIONAL', 'Especializado em carga refrigerada'),
    ('BERCO_004', 'Berco Leste Multiprop', 220, 11.80, 1, 40000, '220V', FALSE, TRUE, TRUE, TRUE, FALSE,
     FALSE, TRUE, FALSE, TRUE, FALSE, FALSE, TRUE, 'ZONA_C', NULL, 300, 14, 'TER-DOM 18H',
     DATE '2026-05-10', DATE '2026-06-25', 'MANUTENCAO', 'Janela preventiva em andamento'),
    ('BERCO_005', 'Berco Norte Backup', 360, 14.80, 2, 70000, '440V trifase', TRUE, TRUE, TRUE, TRUE, FALSE,
     TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, TRUE, 'ZONA_B', 'ZONA_A', 210, 9, 'SEG-DOM 24H',
     DATE '2026-05-08', DATE '2026-07-20', 'OPERACIONAL', 'Reserva operacional para picos de demanda')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO zona_armazenagem (
    codigo, nome, capacidade_total, ocupacao_atual, bloqueada, atualizado_em, observacao
) VALUES
    ('ZONA_A', 'Zona A - Primaria', 100, 72, FALSE, CURRENT_TIMESTAMP, 'Uso equilibrado e com boa folga'),
    ('ZONA_B', 'Zona B - Crítica', 100, 97, TRUE, CURRENT_TIMESTAMP, 'Bloqueio automatico por ocupacao critica'),
    ('ZONA_C', 'Zona C - Expansao', 100, 68, FALSE, CURRENT_TIMESTAMP, 'Area com elasticidade operacional')
ON CONFLICT (codigo) DO NOTHING;

INSERT INTO equipamento_berco (
    identificador, tipo, berco_codigo, status, ultima_verificacao
) VALUES
    ('GUIN-001', 'GUINDASTE', 'BERCO_001', 'OPERACIONAL', CURRENT_TIMESTAMP),
    ('GUIN-002', 'GUINDASTE', 'BERCO_001', 'OPERACIONAL', CURRENT_TIMESTAMP),
    ('GUIN-003', 'GUINDASTE', 'BERCO_003', 'MANUTENCAO', CURRENT_TIMESTAMP),
    ('RTG-010', 'RTG', 'BERCO_002', 'OPERACIONAL', CURRENT_TIMESTAMP)
ON CONFLICT (identificador) DO NOTHING;

INSERT INTO reserva_berco (
    berco_id, navio_codigo, navio_nome, chegada_prevista, saida_prevista, comprimento_navio, calado_navio,
    guinches_requeridos, tipo_carga, zona_armazenagem, tipo_reserva, status, score, motivo, criado_em, atualizado_em
) SELECT
    b.id, 'SHIP-456', 'Atlantic Wave', TIMESTAMP '2026-06-01 08:00:00', TIMESTAMP '2026-06-02 06:00:00', 250, 12.30,
    2, 'CONTAINER', 'ZONA_A', 'ALOCACAO', 'CONFIRMADA', 100, 'Carga compatível e berço livre', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM berco_portuario b WHERE b.codigo = 'BERCO_001'
ON CONFLICT DO NOTHING;

INSERT INTO reserva_berco (
    berco_id, navio_codigo, navio_nome, chegada_prevista, saida_prevista, comprimento_navio, calado_navio,
    guinches_requeridos, tipo_carga, zona_armazenagem, tipo_reserva, status, score, motivo, criado_em, atualizado_em
) SELECT
    b.id, 'SHIP-789', 'CMA CGM Antoine', TIMESTAMP '2026-06-03 12:00:00', TIMESTAMP '2026-06-04 08:00:00', 320, 13.80,
    2, 'REEFER', 'ZONA_A', 'ALOCACAO', 'PROPOSTA', 96, 'Sugestao operacional para janela futura', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP
FROM berco_portuario b WHERE b.codigo = 'BERCO_001'
ON CONFLICT DO NOTHING;
