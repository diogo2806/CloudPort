ALTER TABLE lote_carga
    ADD COLUMN visita_trem_id VARCHAR(80),
    ADD COLUMN vagao_id VARCHAR(120),
    ADD COLUMN posicao_ferroviaria VARCHAR(120),
    ADD COLUMN sequencia_ferroviaria INTEGER,
    ADD COLUMN capacidade_vagao_peso_kg NUMERIC(19,3),
    ADD COLUMN incompatibilidades_ferroviarias VARCHAR(1000),
    ADD COLUMN custodia_ferroviaria VARCHAR(120),
    ADD COLUMN status_ordem_ferroviaria VARCHAR(20);

ALTER TABLE lote_carga
    ADD CONSTRAINT ck_lote_carga_sequencia_ferroviaria
        CHECK (sequencia_ferroviaria IS NULL OR sequencia_ferroviaria > 0),
    ADD CONSTRAINT ck_lote_carga_capacidade_vagao
        CHECK (capacidade_vagao_peso_kg IS NULL OR capacidade_vagao_peso_kg > 0),
    ADD CONSTRAINT ck_lote_carga_status_ordem_ferroviaria
        CHECK (status_ordem_ferroviaria IS NULL
            OR status_ordem_ferroviaria IN ('PENDENTE', 'EM_EXECUCAO', 'CONCLUIDA')),
    ADD CONSTRAINT ck_lote_carga_planejamento_ferroviario
        CHECK (status_ordem_ferroviaria IS NULL OR (
            visita_trem_id IS NOT NULL
            AND vagao_id IS NOT NULL
            AND sequencia_ferroviaria IS NOT NULL
            AND capacidade_vagao_peso_kg IS NOT NULL
            AND custodia_ferroviaria IS NOT NULL));

CREATE TABLE historico_custodia_ferroviaria_carga (
    lote_id UUID NOT NULL,
    status_anterior VARCHAR(20),
    status_novo VARCHAR(20) NOT NULL,
    custodia_anterior VARCHAR(120),
    custodia_nova VARCHAR(120),
    evento VARCHAR(40) NOT NULL,
    motivo VARCHAR(1000),
    responsavel VARCHAR(120) NOT NULL,
    ocorrido_em TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT fk_historico_custodia_ferroviaria_lote
        FOREIGN KEY (lote_id) REFERENCES lote_carga(id) ON DELETE CASCADE,
    CONSTRAINT ck_historico_custodia_status_anterior
        CHECK (status_anterior IS NULL OR status_anterior IN ('PENDENTE', 'EM_EXECUCAO', 'CONCLUIDA')),
    CONSTRAINT ck_historico_custodia_status_novo
        CHECK (status_novo IN ('PENDENTE', 'EM_EXECUCAO', 'CONCLUIDA'))
);

CREATE INDEX idx_lote_carga_visita_trem_ordem
    ON lote_carga(visita_trem_id, status_ordem_ferroviaria, sequencia_ferroviaria);

CREATE INDEX idx_historico_custodia_ferroviaria_lote
    ON historico_custodia_ferroviaria_carga(lote_id, ocorrido_em DESC);
