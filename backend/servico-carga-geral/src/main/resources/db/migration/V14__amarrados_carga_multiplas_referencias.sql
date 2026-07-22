CREATE TABLE amarrado_carga (
    id UUID PRIMARY KEY,
    codigo VARCHAR(100) NOT NULL,
    visita_navio_id VARCHAR(80) NOT NULL,
    integro BOOLEAN NOT NULL DEFAULT TRUE,
    criado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT uk_amarrado_carga_codigo UNIQUE (codigo)
);

CREATE TABLE amarrado_carga_lote (
    amarrado_id UUID NOT NULL,
    lote_id UUID NOT NULL,
    CONSTRAINT pk_amarrado_carga_lote PRIMARY KEY (amarrado_id, lote_id),
    CONSTRAINT uk_lote_amarrado_carga UNIQUE (lote_id),
    CONSTRAINT fk_amarrado_carga_lote_amarrado
        FOREIGN KEY (amarrado_id) REFERENCES amarrado_carga(id),
    CONSTRAINT fk_amarrado_carga_lote_lote
        FOREIGN KEY (lote_id) REFERENCES lote_carga(id)
);

CREATE INDEX idx_amarrado_carga_visita_navio
    ON amarrado_carga(visita_navio_id, atualizado_em DESC);

CREATE INDEX idx_amarrado_carga_lote_amarrado
    ON amarrado_carga_lote(amarrado_id);
