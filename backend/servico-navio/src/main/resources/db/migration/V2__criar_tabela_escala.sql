CREATE TABLE IF NOT EXISTS escala (
    id BIGSERIAL PRIMARY KEY,
    navio_id BIGINT NOT NULL REFERENCES navio (identificador),
    viagem_entrada VARCHAR(20) NOT NULL,
    viagem_saida VARCHAR(20),
    fase VARCHAR(20) NOT NULL,
    chegada_prevista TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atracacao_prevista TIMESTAMP WITHOUT TIME ZONE,
    partida_prevista TIMESTAMP WITHOUT TIME ZONE,
    chegada_efetiva TIMESTAMP WITHOUT TIME ZONE,
    atracacao_efetiva TIMESTAMP WITHOUT TIME ZONE,
    partida_efetiva TIMESTAMP WITHOUT TIME ZONE,
    berco_previsto VARCHAR(20),
    berco_atual VARCHAR(20),
    observacoes VARCHAR(500),
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL
);

CREATE INDEX IF NOT EXISTS idx_escala_fase ON escala (fase);
CREATE INDEX IF NOT EXISTS idx_escala_chegada_prevista ON escala (chegada_prevista);
CREATE INDEX IF NOT EXISTS idx_escala_navio ON escala (navio_id);

-- Migra os dados de escala que hoje residem na tabela navio para uma escala inicial por navio.
INSERT INTO escala (
    navio_id,
    viagem_entrada,
    fase,
    chegada_prevista,
    atracacao_prevista,
    chegada_efetiva,
    atracacao_efetiva,
    partida_efetiva,
    berco_previsto,
    berco_atual,
    observacoes,
    criado_em,
    atualizado_em
)
SELECT
    identificador,
    'MIGRACAO',
    CASE status_operacao
        WHEN 'AGENDADO' THEN 'PREVISTA'
        WHEN 'ATRACADO' THEN 'ATRACADO'
        WHEN 'EM_OPERACAO' THEN 'OPERANDO'
        WHEN 'CONCLUIDO' THEN 'ENCERRADA'
        WHEN 'CANCELADO' THEN 'CANCELADA'
        ELSE 'PREVISTA'
    END,
    data_prevista_atracacao,
    data_prevista_atracacao,
    data_efetiva_atracacao,
    data_efetiva_atracacao,
    data_efetiva_desatracacao,
    berco_previsto,
    berco_atual,
    observacoes,
    NOW(),
    NOW()
FROM navio;

-- Acrescenta atributos físicos do navio (vessel master) inspirados no Vessel Class do Navis N4.
ALTER TABLE navio ADD COLUMN IF NOT EXISTS loa_metros NUMERIC(6, 2);
ALTER TABLE navio ADD COLUMN IF NOT EXISTS calado_maximo_metros NUMERIC(5, 2);
ALTER TABLE navio ADD COLUMN IF NOT EXISTS call_sign VARCHAR(15);

-- Remove os campos de escala que passam a pertencer à tabela escala.
ALTER TABLE navio DROP COLUMN IF EXISTS status_operacao;
ALTER TABLE navio DROP COLUMN IF EXISTS data_prevista_atracacao;
ALTER TABLE navio DROP COLUMN IF EXISTS data_efetiva_atracacao;
ALTER TABLE navio DROP COLUMN IF EXISTS data_efetiva_desatracacao;
ALTER TABLE navio DROP COLUMN IF EXISTS berco_previsto;
ALTER TABLE navio DROP COLUMN IF EXISTS berco_atual;
ALTER TABLE navio DROP COLUMN IF EXISTS observacoes;
