ALTER TABLE item_operacao_navio ADD COLUMN IF NOT EXISTS conteiner_patio_id BIGINT;
ALTER TABLE item_operacao_navio ADD COLUMN IF NOT EXISTS carga_patio_id BIGINT;
ALTER TABLE item_operacao_navio ADD COLUMN IF NOT EXISTS ordem_trabalho_patio_id BIGINT;
ALTER TABLE item_operacao_navio ADD COLUMN IF NOT EXISTS movimento_patio_id BIGINT;
ALTER TABLE item_operacao_navio ADD COLUMN IF NOT EXISTS posicao_patio_planejada VARCHAR(120);
ALTER TABLE item_operacao_navio ADD COLUMN IF NOT EXISTS posicao_patio_real VARCHAR(120);
ALTER TABLE item_operacao_navio ADD COLUMN IF NOT EXISTS status_integracao_patio VARCHAR(30) NOT NULL DEFAULT 'NAO_GERADO';

CREATE TABLE IF NOT EXISTS reserva_posicao_patio_navio (
    id BIGSERIAL PRIMARY KEY,
    visita_navio_id BIGINT NOT NULL REFERENCES visita_navio (id) ON DELETE CASCADE,
    item_operacao_navio_id BIGINT NOT NULL REFERENCES item_operacao_navio (id) ON DELETE CASCADE,
    posicao_patio_id VARCHAR(120) NOT NULL,
    bloco VARCHAR(40),
    linha INTEGER,
    coluna INTEGER,
    camada VARCHAR(40),
    tipo_reserva VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL,
    motivo_cancelamento VARCHAR(500),
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT ck_reserva_patio_navio_tipo CHECK (tipo_reserva IN ('TENTATIVA', 'DEFINITIVA')),
    CONSTRAINT ck_reserva_patio_navio_status CHECK (status IN ('ATIVA', 'CONSUMIDA', 'CANCELADA', 'EXPIRADA'))
);

CREATE INDEX IF NOT EXISTS idx_item_navio_patio_ordem ON item_operacao_navio (ordem_trabalho_patio_id);
CREATE INDEX IF NOT EXISTS idx_item_navio_status_integracao_patio ON item_operacao_navio (status_integracao_patio);
CREATE INDEX IF NOT EXISTS idx_reserva_patio_navio_visita ON reserva_posicao_patio_navio (visita_navio_id, status);
CREATE INDEX IF NOT EXISTS idx_reserva_patio_navio_item ON reserva_posicao_patio_navio (item_operacao_navio_id, status);
CREATE INDEX IF NOT EXISTS idx_reserva_patio_navio_posicao ON reserva_posicao_patio_navio (posicao_patio_id, status);
