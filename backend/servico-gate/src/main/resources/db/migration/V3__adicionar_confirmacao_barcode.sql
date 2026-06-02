-- Add barcode confirmation columns to gate_pass table
ALTER TABLE gate_pass
ADD COLUMN codigo_barcode VARCHAR(50),
ADD COLUMN data_confirmacao_barcode TIMESTAMP,
ADD COLUMN status_confirmacao_barcode VARCHAR(40),
ADD COLUMN motivo_rejeicao_barcode VARCHAR(500);

-- Create index for token lookup performance
CREATE INDEX idx_gate_pass_token ON gate_pass(token);

-- Create index for barcode status queries
CREATE INDEX idx_gate_pass_status_confirmacao ON gate_pass(status_confirmacao_barcode);
