CREATE EXTENSION IF NOT EXISTS btree_gist;

ALTER TABLE visita_trem
    ADD COLUMN IF NOT EXISTS posicao_ferroviaria_atual VARCHAR(120);

CREATE TABLE movimento_ferroviario_interno (
    id BIGSERIAL PRIMARY KEY,
    codigo_movimento VARCHAR(36) NOT NULL,
    visita_trem_id BIGINT NOT NULL,
    origem VARCHAR(120) NOT NULL,
    destino VARCHAR(120) NOT NULL,
    inicio_planejado TIMESTAMP NOT NULL,
    fim_planejado TIMESTAMP NOT NULL,
    estado VARCHAR(24) NOT NULL,
    reserva_ativa BOOLEAN NOT NULL DEFAULT FALSE,
    motivo_cancelamento VARCHAR(500),
    planejado_por VARCHAR(120) NOT NULL,
    autorizado_por VARCHAR(120),
    iniciado_por VARCHAR(120),
    concluido_por VARCHAR(120),
    cancelado_por VARCHAR(120),
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    autorizado_em TIMESTAMP,
    iniciado_em TIMESTAMP,
    concluido_em TIMESTAMP,
    cancelado_em TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uk_movimento_ferroviario_codigo UNIQUE (codigo_movimento),
    CONSTRAINT fk_movimento_ferroviario_visita FOREIGN KEY (visita_trem_id) REFERENCES visita_trem (id),
    CONSTRAINT ck_movimento_ferroviario_janela CHECK (fim_planejado > inicio_planejado),
    CONSTRAINT ck_movimento_ferroviario_estado CHECK (estado IN ('PLANEJADO', 'AUTORIZADO', 'EM_EXECUCAO', 'CONCLUIDO', 'CANCELADO')),
    CONSTRAINT ck_movimento_ferroviario_origem_destino CHECK (UPPER(origem) <> UPPER(destino))
);

CREATE INDEX idx_movimento_ferroviario_visita ON movimento_ferroviario_interno (visita_trem_id, criado_em DESC);
CREATE INDEX idx_movimento_ferroviario_estado ON movimento_ferroviario_interno (estado, inicio_planejado);

ALTER TABLE movimento_ferroviario_interno
    ADD CONSTRAINT ex_movimento_visita_periodo
    EXCLUDE USING gist (
        visita_trem_id WITH =,
        tsrange(inicio_planejado, fim_planejado, '[)') WITH &&
    ) WHERE (reserva_ativa);

CREATE TABLE reserva_recurso_ferroviario (
    id BIGSERIAL PRIMARY KEY,
    movimento_id BIGINT NOT NULL,
    tipo_recurso VARCHAR(20) NOT NULL,
    codigo_recurso VARCHAR(80) NOT NULL,
    inicio_reserva TIMESTAMP NOT NULL,
    fim_reserva TIMESTAMP NOT NULL,
    ativo BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_reserva_recurso_movimento FOREIGN KEY (movimento_id) REFERENCES movimento_ferroviario_interno (id) ON DELETE CASCADE,
    CONSTRAINT uk_reserva_recurso_movimento UNIQUE (movimento_id, tipo_recurso, codigo_recurso),
    CONSTRAINT ck_reserva_recurso_tipo CHECK (tipo_recurso IN ('ROTA', 'LINHA', 'TRECHO', 'SWITCH')),
    CONSTRAINT ck_reserva_recurso_janela CHECK (fim_reserva > inicio_reserva)
);

CREATE INDEX idx_reserva_recurso_movimento ON reserva_recurso_ferroviario (movimento_id);
CREATE INDEX idx_reserva_recurso_consulta ON reserva_recurso_ferroviario (tipo_recurso, codigo_recurso, ativo);

ALTER TABLE reserva_recurso_ferroviario
    ADD CONSTRAINT ex_recurso_ferroviario_periodo
    EXCLUDE USING gist (
        tipo_recurso WITH =,
        codigo_recurso WITH =,
        tsrange(inicio_reserva, fim_reserva, '[)') WITH &&
    ) WHERE (ativo);
