CREATE TABLE saldo_posicao_cargo_lot (
    id UUID PRIMARY KEY,
    capacidade_id UUID NOT NULL,
    lote_id UUID NOT NULL,
    quantidade NUMERIC(19,3) NOT NULL DEFAULT 0,
    volume_m3 NUMERIC(19,3) NOT NULL DEFAULT 0,
    peso_kg NUMERIC(19,3) NOT NULL DEFAULT 0,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_saldo_posicao_cargo_lot_capacidade
        FOREIGN KEY (capacidade_id) REFERENCES capacidade_posicao_cargo_lot(id),
    CONSTRAINT uk_saldo_posicao_cargo_lot
        UNIQUE (capacidade_id, lote_id),
    CONSTRAINT ck_saldo_posicao_cargo_lot_quantidade
        CHECK (quantidade >= 0),
    CONSTRAINT ck_saldo_posicao_cargo_lot_volume
        CHECK (volume_m3 >= 0),
    CONSTRAINT ck_saldo_posicao_cargo_lot_peso
        CHECK (peso_kg >= 0)
);

INSERT INTO saldo_posicao_cargo_lot (
    id,
    capacidade_id,
    lote_id,
    quantidade,
    volume_m3,
    peso_kg,
    criado_em,
    atualizado_em,
    versao
)
SELECT
    gen_random_uuid(),
    capacidade_id,
    lote_id,
    SUM(quantidade),
    SUM(volume_m3),
    SUM(peso_kg),
    CURRENT_TIMESTAMP,
    CURRENT_TIMESTAMP,
    0
FROM reserva_capacidade_cargo_lot
WHERE status = 'CONFIRMADA'
GROUP BY capacidade_id, lote_id;

CREATE INDEX idx_saldo_posicao_cargo_lot_capacidade
    ON saldo_posicao_cargo_lot(capacidade_id);
CREATE INDEX idx_saldo_posicao_cargo_lot_lote
    ON saldo_posicao_cargo_lot(lote_id);
