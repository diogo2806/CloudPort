-- Plano de estiva (stowage plan) por escala e atribuições de contêineres às
-- células bay/row/tier do navio. O instante de embarque efetivo de cada
-- atribuição representa a baixa do estoque do pátio.

CREATE TABLE IF NOT EXISTS plano_estiva (
    id BIGSERIAL PRIMARY KEY,
    escala_id BIGINT NOT NULL UNIQUE REFERENCES escala (id),
    status VARCHAR(20) NOT NULL,
    baias INTEGER NOT NULL,
    fileiras INTEGER NOT NULL,
    camadas INTEGER NOT NULL,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE TABLE IF NOT EXISTS atribuicao_estiva (
    id BIGSERIAL PRIMARY KEY,
    plano_id BIGINT NOT NULL REFERENCES plano_estiva (id),
    codigo_conteiner VARCHAR(20) NOT NULL,
    tipo_carga VARCHAR(20) NOT NULL,
    peso_toneladas NUMERIC(7, 2),
    baia INTEGER NOT NULL,
    fileira INTEGER NOT NULL,
    camada INTEGER NOT NULL,
    posicao_patio_origem VARCHAR(40),
    sequencia_embarque INTEGER,
    embarcado BOOLEAN NOT NULL DEFAULT FALSE,
    embarcado_em TIMESTAMP WITHOUT TIME ZONE,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_atribuicao_celula UNIQUE (plano_id, baia, fileira, camada),
    CONSTRAINT uk_atribuicao_conteiner UNIQUE (plano_id, codigo_conteiner)
);

CREATE INDEX IF NOT EXISTS idx_atribuicao_estiva_plano ON atribuicao_estiva (plano_id);
CREATE INDEX IF NOT EXISTS idx_atribuicao_estiva_embarcado ON atribuicao_estiva (embarcado);
