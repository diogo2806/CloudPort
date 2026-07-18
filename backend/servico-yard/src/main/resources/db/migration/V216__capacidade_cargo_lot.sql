CREATE TABLE capacidade_posicao_cargo_lot (
    id UUID PRIMARY KEY,
    posicao VARCHAR(120) NOT NULL UNIQUE,
    capacidade_quantidade NUMERIC(19,3) NOT NULL,
    capacidade_volume_m3 NUMERIC(19,3) NOT NULL,
    capacidade_peso_kg NUMERIC(19,3) NOT NULL,
    restricoes VARCHAR(1000),
    ativa BOOLEAN NOT NULL,
    versao BIGINT NOT NULL
);

CREATE TABLE reserva_capacidade_cargo_lot (
    id UUID PRIMARY KEY,
    command_id UUID NOT NULL UNIQUE,
    capacidade_id UUID NOT NULL,
    lote_id UUID NOT NULL,
    quantidade NUMERIC(19,3) NOT NULL,
    volume_m3 NUMERIC(19,3) NOT NULL,
    peso_kg NUMERIC(19,3) NOT NULL,
    status VARCHAR(20) NOT NULL,
    usuario_reserva VARCHAR(120) NOT NULL,
    reservado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    usuario_finalizacao VARCHAR(120),
    motivo_finalizacao VARCHAR(1000),
    finalizado_em TIMESTAMP WITH TIME ZONE,
    CONSTRAINT fk_reserva_capacidade_cargo_lot_posicao
        FOREIGN KEY (capacidade_id) REFERENCES capacidade_posicao_cargo_lot(id),
    CONSTRAINT ck_reserva_capacidade_cargo_lot_status
        CHECK (status IN ('RESERVADA', 'CONFIRMADA', 'CANCELADA'))
);

CREATE INDEX idx_reserva_capacidade_cargo_lot_posicao_status
    ON reserva_capacidade_cargo_lot(capacidade_id, status);
CREATE INDEX idx_reserva_capacidade_cargo_lot_lote
    ON reserva_capacidade_cargo_lot(lote_id, reservado_em DESC);
