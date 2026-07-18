ALTER TABLE alerta
    ADD COLUMN IF NOT EXISTS data_reconhecimento TIMESTAMP,
    ADD COLUMN IF NOT EXISTS reconhecido_por VARCHAR(120),
    ADD COLUMN IF NOT EXISTS resolvido_por VARCHAR(120);

CREATE INDEX IF NOT EXISTS idx_alerta_status_severidade_data
    ON alerta(status, severidade, data_gerada DESC);

CREATE INDEX IF NOT EXISTS idx_alerta_status_reconhecimento
    ON alerta(status, data_reconhecimento);
