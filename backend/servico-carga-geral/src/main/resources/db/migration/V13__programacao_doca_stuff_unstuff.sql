CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE programacao_doca_carga (
    id UUID PRIMARY KEY,
    operacao_id UUID NOT NULL,
    conteiner_id VARCHAR(80) NOT NULL,
    doca_id VARCHAR(80) NOT NULL,
    area_espera_id VARCHAR(80) NOT NULL,
    recurso_id VARCHAR(120) NOT NULL,
    janela_inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    janela_fim TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,
    reservado_por VARCHAR(120) NOT NULL,
    reservado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    observacao_reserva VARCHAR(1000),
    iniciado_por VARCHAR(120),
    iniciado_em TIMESTAMP WITH TIME ZONE,
    concluido_por VARCHAR(120),
    concluido_em TIMESTAMP WITH TIME ZONE,
    cancelado_por VARCHAR(120),
    cancelado_em TIMESTAMP WITH TIME ZONE,
    motivo_cancelamento VARCHAR(1000),
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_programacao_doca_operacao
        FOREIGN KEY (operacao_id) REFERENCES operacao_stuff_unstuff(id),
    CONSTRAINT uk_programacao_doca_operacao UNIQUE (operacao_id),
    CONSTRAINT ck_programacao_doca_janela CHECK (janela_fim > janela_inicio),
    CONSTRAINT ck_programacao_doca_status CHECK (
        status IN ('RESERVADA', 'EM_USO', 'CONCLUIDA', 'CANCELADA')
    ),
    CONSTRAINT ck_programacao_doca_inicio CHECK (
        status = 'RESERVADA' OR (iniciado_por IS NOT NULL AND iniciado_em IS NOT NULL)
    ),
    CONSTRAINT ck_programacao_doca_conclusao CHECK (
        status <> 'CONCLUIDA' OR (concluido_por IS NOT NULL AND concluido_em IS NOT NULL)
    ),
    CONSTRAINT ck_programacao_doca_cancelamento CHECK (
        status <> 'CANCELADA'
        OR (cancelado_por IS NOT NULL AND cancelado_em IS NOT NULL AND motivo_cancelamento IS NOT NULL)
    )
);

CREATE TABLE reserva_lote_programacao_doca_carga (
    id UUID PRIMARY KEY,
    programacao_id UUID NOT NULL,
    lote_id UUID NOT NULL,
    lote_codigo VARCHAR(80) NOT NULL,
    quantidade_reservada NUMERIC(19,3) NOT NULL,
    volume_reservado_m3 NUMERIC(19,3) NOT NULL,
    peso_reservado_kg NUMERIC(19,3) NOT NULL,
    janela_inicio TIMESTAMP WITH TIME ZONE NOT NULL,
    janela_fim TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,
    CONSTRAINT fk_reserva_lote_programacao
        FOREIGN KEY (programacao_id) REFERENCES programacao_doca_carga(id) ON DELETE CASCADE,
    CONSTRAINT fk_reserva_lote_carga
        FOREIGN KEY (lote_id) REFERENCES lote_carga(id),
    CONSTRAINT uk_reserva_lote_programacao UNIQUE (programacao_id, lote_id),
    CONSTRAINT ck_reserva_lote_programacao_janela CHECK (janela_fim > janela_inicio),
    CONSTRAINT ck_reserva_lote_programacao_valores CHECK (
        quantidade_reservada > 0
        AND volume_reservado_m3 >= 0
        AND peso_reservado_kg >= 0
    ),
    CONSTRAINT ck_reserva_lote_programacao_status CHECK (
        status IN ('RESERVADA', 'EM_USO', 'CONCLUIDA', 'CANCELADA')
    )
);

CREATE INDEX idx_programacao_doca_janela
    ON programacao_doca_carga(janela_inicio, janela_fim, status);
CREATE INDEX idx_programacao_doca_operacao
    ON programacao_doca_carga(operacao_id);
CREATE INDEX idx_reserva_lote_programacao_lote
    ON reserva_lote_programacao_doca_carga(lote_id, janela_inicio, janela_fim, status);

ALTER TABLE programacao_doca_carga
    ADD CONSTRAINT ex_programacao_doca_periodo
    EXCLUDE USING gist (
        doca_id WITH =,
        tstzrange(janela_inicio, janela_fim, '[)') WITH &&
    ) WHERE (status IN ('RESERVADA', 'EM_USO'));

ALTER TABLE programacao_doca_carga
    ADD CONSTRAINT ex_programacao_area_espera_periodo
    EXCLUDE USING gist (
        area_espera_id WITH =,
        tstzrange(janela_inicio, janela_fim, '[)') WITH &&
    ) WHERE (status IN ('RESERVADA', 'EM_USO'));

ALTER TABLE programacao_doca_carga
    ADD CONSTRAINT ex_programacao_recurso_periodo
    EXCLUDE USING gist (
        recurso_id WITH =,
        tstzrange(janela_inicio, janela_fim, '[)') WITH &&
    ) WHERE (status IN ('RESERVADA', 'EM_USO'));

ALTER TABLE programacao_doca_carga
    ADD CONSTRAINT ex_programacao_conteiner_periodo
    EXCLUDE USING gist (
        conteiner_id WITH =,
        tstzrange(janela_inicio, janela_fim, '[)') WITH &&
    ) WHERE (status IN ('RESERVADA', 'EM_USO'));

ALTER TABLE reserva_lote_programacao_doca_carga
    ADD CONSTRAINT ex_reserva_lote_programacao_periodo
    EXCLUDE USING gist (
        lote_id WITH =,
        tstzrange(janela_inicio, janela_fim, '[)') WITH &&
    ) WHERE (status IN ('RESERVADA', 'EM_USO'));
