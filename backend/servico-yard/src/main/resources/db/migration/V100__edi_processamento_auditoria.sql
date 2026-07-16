CREATE TABLE edi_processamento (
    id BIGSERIAL PRIMARY KEY,
    tipo_mensagem VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    conteudo_original TEXT NOT NULL,
    codigo_navio VARCHAR(50),
    codigo_viagem VARCHAR(30),
    referencia_mensagem VARCHAR(100),
    correlation_id VARCHAR(100),
    motivo_rejeicao VARCHAR(2000),
    motivo_reprocessamento VARCHAR(500),
    usuario_reprocessamento VARCHAR(150),
    reprocessamento_de_id BIGINT,
    tentativa INTEGER NOT NULL DEFAULT 1,
    bay_plan_id BIGINT,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_edi_processamento_origem
        FOREIGN KEY (reprocessamento_de_id) REFERENCES edi_processamento(id),
    CONSTRAINT ck_edi_processamento_tentativa CHECK (tentativa >= 1),
    CONSTRAINT ck_edi_processamento_status CHECK (
        status IN ('RECEBIDO', 'PROCESSANDO', 'CONCLUIDO', 'REJEITADO')
    ),
    CONSTRAINT ck_edi_processamento_tipo CHECK (
        tipo_mensagem IN ('BAPLIE', 'COPRAR', 'COARRI', 'VERMAS')
    )
);

CREATE INDEX idx_edi_processamento_tipo_status
    ON edi_processamento(tipo_mensagem, status, criado_em DESC);
CREATE INDEX idx_edi_processamento_reprocessamento
    ON edi_processamento(reprocessamento_de_id);
CREATE INDEX idx_edi_processamento_correlation
    ON edi_processamento(correlation_id);
