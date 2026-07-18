CREATE TABLE identificacao_cargo_lot (
    id UUID PRIMARY KEY,
    codigo VARCHAR(160) NOT NULL UNIQUE,
    tipo VARCHAR(30) NOT NULL,
    lote_id UUID NOT NULL,
    embalagem_referencia VARCHAR(160),
    ativo BOOLEAN NOT NULL,
    registrado_por VARCHAR(120) NOT NULL,
    registrado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_identificacao_cargo_lot_lote FOREIGN KEY (lote_id) REFERENCES lote_carga(id),
    CONSTRAINT ck_identificacao_cargo_lot_tipo CHECK (tipo IN ('CODIGO_BARRAS', 'QR_CODE'))
);

CREATE INDEX idx_identificacao_cargo_lot_lote
    ON identificacao_cargo_lot(lote_id, registrado_em DESC);
