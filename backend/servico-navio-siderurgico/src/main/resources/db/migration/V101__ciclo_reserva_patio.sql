ALTER TABLE item_operacao_navio ADD COLUMN IF NOT EXISTS altura_carga_metros NUMERIC(8, 3);

ALTER TABLE item_operacao_navio DROP CONSTRAINT IF EXISTS ck_item_operacao_altura_carga;
ALTER TABLE item_operacao_navio ADD CONSTRAINT ck_item_operacao_altura_carga
    CHECK (altura_carga_metros IS NULL OR altura_carga_metros > 0);

ALTER TABLE reserva_posicao_patio_navio ADD COLUMN IF NOT EXISTS expira_em TIMESTAMP WITHOUT TIME ZONE;
ALTER TABLE reserva_posicao_patio_navio ADD COLUMN IF NOT EXISTS reserva_anterior_id BIGINT;

ALTER TABLE reserva_posicao_patio_navio DROP CONSTRAINT IF EXISTS fk_reserva_patio_anterior;
ALTER TABLE reserva_posicao_patio_navio ADD CONSTRAINT fk_reserva_patio_anterior
    FOREIGN KEY (reserva_anterior_id) REFERENCES reserva_posicao_patio_navio (id);

CREATE INDEX IF NOT EXISTS idx_reserva_patio_navio_expiracao
    ON reserva_posicao_patio_navio (status, expira_em);
CREATE INDEX IF NOT EXISTS idx_reserva_patio_navio_anterior
    ON reserva_posicao_patio_navio (reserva_anterior_id);
