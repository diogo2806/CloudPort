CREATE TABLE IF NOT EXISTS reefer_telemetria_patio (
    id BIGSERIAL PRIMARY KEY,
    conteiner_id BIGINT NOT NULL UNIQUE,
    temperatura_atual_celsius NUMERIC(7,2) NOT NULL,
    temperatura_minima_celsius NUMERIC(7,2) NOT NULL,
    temperatura_maxima_celsius NUMERIC(7,2) NOT NULL,
    ligado BOOLEAN NOT NULL,
    registrado_em TIMESTAMP NOT NULL,
    CONSTRAINT fk_reefer_telemetria_conteiner
        FOREIGN KEY (conteiner_id) REFERENCES conteiner_patio(id)
);

CREATE INDEX IF NOT EXISTS idx_reefer_telemetria_registrado_em
    ON reefer_telemetria_patio (registrado_em);
