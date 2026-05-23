CREATE TABLE IF NOT EXISTS instrucao_movimentacao (
    id BIGSERIAL PRIMARY KEY,
    codigo_conteiner VARCHAR(30) NOT NULL,
    iso_tipo VARCHAR(10),
    comprimento_pes INTEGER,
    line_operator VARCHAR(60),
    porto_origem VARCHAR(10),
    porto_destino VARCHAR(10),
    peso_kg INTEGER,
    tipo_move VARCHAR(30) NOT NULL,
    posicao_origem VARCHAR(60),
    posicao_destino VARCHAR(60),
    equipamento_id BIGINT REFERENCES equipamento_patio (id),
    fila_trabalho VARCHAR(40),
    sequencia INTEGER NOT NULL DEFAULT 0,
    prioridade_fetch BOOLEAN NOT NULL DEFAULT FALSE,
    move_twin BOOLEAN NOT NULL DEFAULT FALSE,
    requer_energia BOOLEAN NOT NULL DEFAULT FALSE,
    perigoso BOOLEAN NOT NULL DEFAULT FALSE,
    fora_de_bitola BOOLEAN NOT NULL DEFAULT FALSE,
    status VARCHAR(20) NOT NULL,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    concluido_em TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_instrucao_mov_equipamento ON instrucao_movimentacao (equipamento_id);
CREATE INDEX IF NOT EXISTS idx_instrucao_mov_status ON instrucao_movimentacao (status);
