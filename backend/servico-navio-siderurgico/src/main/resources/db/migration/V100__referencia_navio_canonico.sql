ALTER TABLE navio_siderurgico
    ADD COLUMN IF NOT EXISTS navio_cadastro_id BIGINT;

CREATE UNIQUE INDEX IF NOT EXISTS uk_navio_siderurgico_cadastro
    ON navio_siderurgico (navio_cadastro_id)
    WHERE navio_cadastro_id IS NOT NULL;
