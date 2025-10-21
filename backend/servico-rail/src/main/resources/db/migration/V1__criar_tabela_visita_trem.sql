CREATE TABLE IF NOT EXISTS visita_trem (
    id BIGSERIAL PRIMARY KEY,
    identificador_trem VARCHAR(40) NOT NULL,
    operadora_ferroviaria VARCHAR(80) NOT NULL,
    hora_chegada_prevista TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    hora_partida_prevista TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    status_visita VARCHAR(20) NOT NULL,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX IF NOT EXISTS idx_visita_trem_identificador ON visita_trem (identificador_trem);
CREATE INDEX IF NOT EXISTS idx_visita_trem_janela ON visita_trem (hora_chegada_prevista, hora_partida_prevista);
