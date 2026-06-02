-- Create reconciliation_barcode table
CREATE TABLE reconciliacao_barcode (
    id SERIAL PRIMARY KEY,
    gate_pass_id BIGINT NOT NULL REFERENCES gate_pass(id) ON DELETE CASCADE,
    tipo_desinconia VARCHAR(50) NOT NULL,
    descricao VARCHAR(1000),
    barcode_esperado VARCHAR(50),
    barcode_recebido VARCHAR(50),
    status_tos VARCHAR(50),
    status_local VARCHAR(50),
    tempo_pendencia_horas INTEGER,
    detectado_em TIMESTAMP NOT NULL,
    resolvido_em TIMESTAMP,
    resolucao VARCHAR(500),
    alerta_enviado BOOLEAN DEFAULT FALSE NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(80),
    updated_by VARCHAR(80)
);

-- Create indexes for reconciliation queries
CREATE INDEX idx_reconciliacao_gate_pass ON reconciliacao_barcode(gate_pass_id);
CREATE INDEX idx_reconciliacao_tipo ON reconciliacao_barcode(tipo_desinconia);
CREATE INDEX idx_reconciliacao_resolvido ON reconciliacao_barcode(resolvido_em);
CREATE INDEX idx_reconciliacao_detectado ON reconciliacao_barcode(detectado_em DESC);
CREATE INDEX idx_reconciliacao_alerta ON reconciliacao_barcode(alerta_enviado, resolvido_em);

-- Create view for unresolved reconciliations
CREATE VIEW reconciliacao_nao_resolvida AS
SELECT
    r.id,
    r.gate_pass_id,
    gp.codigo,
    r.tipo_desinconia,
    r.descricao,
    r.tempo_pendencia_horas,
    r.detectado_em,
    EXTRACT(HOUR FROM (CURRENT_TIMESTAMP - r.detectado_em)) as horas_sem_resolucao,
    r.alerta_enviado
FROM reconciliacao_barcode r
JOIN gate_pass gp ON r.gate_pass_id = gp.id
WHERE r.resolvido_em IS NULL
ORDER BY r.detectado_em DESC;
