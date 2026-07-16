CREATE TABLE IF NOT EXISTS telemetria_equipamento_patio (
    id BIGSERIAL PRIMARY KEY,
    equipamento_id BIGINT NOT NULL UNIQUE REFERENCES equipamento_patio (id) ON DELETE CASCADE,
    latitude DOUBLE PRECISION,
    longitude DOUBLE PRECISION,
    coordenada_x DOUBLE PRECISION,
    coordenada_y DOUBLE PRECISION,
    heading DOUBLE PRECISION,
    posicao_mais_proxima VARCHAR(80),
    distancia_posicao_centimetros INTEGER,
    dentro_da_posicao BOOLEAN,
    origem VARCHAR(80) NOT NULL,
    operador_vmt VARCHAR(120),
    status_vmt VARCHAR(40),
    work_instruction_atual_id BIGINT,
    sequencia BIGINT NOT NULL,
    capturado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    recebido_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT ck_telemetria_distancia_nao_negativa CHECK (
        distancia_posicao_centimetros IS NULL OR distancia_posicao_centimetros >= 0
    ),
    CONSTRAINT ck_telemetria_coordenada CHECK (
        (latitude IS NOT NULL AND longitude IS NOT NULL)
        OR (coordenada_x IS NOT NULL AND coordenada_y IS NOT NULL)
    )
);

CREATE INDEX IF NOT EXISTS idx_telemetria_equipamento_capturado
    ON telemetria_equipamento_patio (equipamento_id, capturado_em DESC);

CREATE INDEX IF NOT EXISTS idx_telemetria_work_instruction
    ON telemetria_equipamento_patio (work_instruction_atual_id)
    WHERE work_instruction_atual_id IS NOT NULL;
