ALTER TABLE posicao_patio
    ADD COLUMN IF NOT EXISTS reserva_chave VARCHAR(120);

ALTER TABLE posicao_patio
    ADD COLUMN IF NOT EXISTS reserva_codigo_conteiner VARCHAR(30);

ALTER TABLE posicao_patio
    ADD COLUMN IF NOT EXISTS reserva_expira_em TIMESTAMP WITHOUT TIME ZONE;

ALTER TABLE ordem_trabalho_patio
    ADD COLUMN IF NOT EXISTS chave_idempotencia VARCHAR(120);

CREATE UNIQUE INDEX IF NOT EXISTS uk_posicao_patio_reserva_chave
    ON posicao_patio (reserva_chave)
    WHERE reserva_chave IS NOT NULL;

CREATE UNIQUE INDEX IF NOT EXISTS uk_ordem_trabalho_patio_chave_idempotencia
    ON ordem_trabalho_patio (chave_idempotencia)
    WHERE chave_idempotencia IS NOT NULL;

CREATE INDEX IF NOT EXISTS idx_posicao_patio_reserva_expiracao
    ON posicao_patio (reserva_expira_em)
    WHERE reserva_chave IS NOT NULL;
