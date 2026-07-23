CREATE TABLE trem_mestre (
    id BIGSERIAL PRIMARY KEY,
    versao BIGINT NOT NULL DEFAULT 0,
    identificador VARCHAR(40) NOT NULL,
    operadora_ferroviaria VARCHAR(80) NOT NULL,
    descricao VARCHAR(120),
    ativo BOOLEAN NOT NULL DEFAULT TRUE,
    observacoes VARCHAR(500),
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    criado_por VARCHAR(120),
    atualizado_por VARCHAR(120),
    CONSTRAINT uk_trem_mestre_operadora_identificador UNIQUE (operadora_ferroviaria, identificador)
);

CREATE TABLE trem_mestre_vagao (
    trem_mestre_id BIGINT NOT NULL REFERENCES trem_mestre(id) ON DELETE CASCADE,
    ordem_vagao INTEGER NOT NULL,
    posicao_no_trem INTEGER NOT NULL,
    identificador_vagao VARCHAR(35) NOT NULL,
    tipo_vagao VARCHAR(40),
    CONSTRAINT uk_trem_mestre_vagao_posicao UNIQUE (trem_mestre_id, posicao_no_trem),
    CONSTRAINT uk_trem_mestre_vagao_identificador UNIQUE (trem_mestre_id, identificador_vagao)
);

ALTER TABLE visita_trem ADD COLUMN trem_mestre_id BIGINT;
ALTER TABLE visita_trem ADD CONSTRAINT fk_visita_trem_mestre FOREIGN KEY (trem_mestre_id) REFERENCES trem_mestre(id);
CREATE INDEX idx_visita_trem_mestre_id ON visita_trem(trem_mestre_id);
