CREATE TABLE escala_descarga (
    escala_id BIGINT NOT NULL,
    codigo_conteiner VARCHAR(20) NOT NULL,
    status_operacao VARCHAR(20) NOT NULL,
    ordem_descarga INTEGER NOT NULL,
    CONSTRAINT fk_escala_descarga FOREIGN KEY (escala_id)
        REFERENCES escala (id) ON DELETE CASCADE
);

CREATE TABLE escala_carga (
    escala_id BIGINT NOT NULL,
    codigo_conteiner VARCHAR(20) NOT NULL,
    status_operacao VARCHAR(20) NOT NULL,
    ordem_carga INTEGER NOT NULL,
    CONSTRAINT fk_escala_carga FOREIGN KEY (escala_id)
        REFERENCES escala (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_escala_descarga_conteiner ON escala_descarga (escala_id, codigo_conteiner);
CREATE UNIQUE INDEX uk_escala_carga_conteiner ON escala_carga (escala_id, codigo_conteiner);

CREATE TABLE ordem_movimentacao_navio (
    id BIGSERIAL PRIMARY KEY,
    escala_id BIGINT NOT NULL,
    codigo_conteiner VARCHAR(20) NOT NULL,
    tipo_movimentacao VARCHAR(20) NOT NULL,
    status_movimentacao VARCHAR(20) NOT NULL,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT fk_ordem_navio_escala FOREIGN KEY (escala_id)
        REFERENCES escala (id) ON DELETE CASCADE,
    CONSTRAINT uk_ordem_navio_escala_conteiner_tipo
        UNIQUE (escala_id, codigo_conteiner, tipo_movimentacao)
);

CREATE INDEX idx_ordem_navio_escala_status ON ordem_movimentacao_navio (escala_id, status_movimentacao);
