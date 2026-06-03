-- ============================================================
-- V3: Consolidação de entidades do pátio
--
-- Objetivos:
--   1. Adicionar campos operacionais à conteiner_patio,
--      absorvendo a entidade duplicada 'conteiner'.
--   2. Tornar carga_id opcional (tipo de carga migrado para
--      coluna enum tipo_carga).
--   3. Adicionar campos de auditoria a movimento_patio,
--      absorvendo a entidade duplicada 'historico_operacao_conteiner'.
--   4. Criar ordem_trabalho_patio (estava ausente das migrações).
-- ============================================================

-- 1. Campos operacionais em conteiner_patio
ALTER TABLE conteiner_patio ADD COLUMN IF NOT EXISTS peso_toneladas  NUMERIC(10,3);
ALTER TABLE conteiner_patio ADD COLUMN IF NOT EXISTS restricoes       VARCHAR(255);
ALTER TABLE conteiner_patio ADD COLUMN IF NOT EXISTS versao           BIGINT NOT NULL DEFAULT 0;
ALTER TABLE conteiner_patio ADD COLUMN IF NOT EXISTS tipo_carga       VARCHAR(40);

-- 2. carga_id passa a ser opcional (tipo de carga via coluna enum)
ALTER TABLE conteiner_patio ALTER COLUMN carga_id DROP NOT NULL;

-- 3. Campos de auditoria em movimento_patio
ALTER TABLE movimento_patio ADD COLUMN IF NOT EXISTS responsavel      VARCHAR(120);
ALTER TABLE movimento_patio ADD COLUMN IF NOT EXISTS posicao_anterior VARCHAR(120);
ALTER TABLE movimento_patio ADD COLUMN IF NOT EXISTS posicao_atual    VARCHAR(120);

-- 4. Criar tabela de ordens de trabalho do pátio
CREATE TABLE IF NOT EXISTS ordem_trabalho_patio (
    id                       BIGSERIAL PRIMARY KEY,
    conteiner_id             BIGINT REFERENCES conteiner_patio(id) ON DELETE SET NULL,
    codigo_conteiner         VARCHAR(30) NOT NULL,
    tipo_carga               VARCHAR(40),
    destino                  VARCHAR(60) NOT NULL,
    linha_destino            INTEGER NOT NULL,
    coluna_destino           INTEGER NOT NULL,
    camada_destino           VARCHAR(40) NOT NULL,
    tipo_movimento           VARCHAR(30) NOT NULL,
    status_ordem             VARCHAR(30) NOT NULL,
    status_conteiner_destino VARCHAR(30),
    criado_em                TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em            TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    concluido_em             TIMESTAMP WITHOUT TIME ZONE
);

CREATE INDEX IF NOT EXISTS idx_ordem_codigo_status
    ON ordem_trabalho_patio (codigo_conteiner, status_ordem);

CREATE INDEX IF NOT EXISTS idx_ordem_status_criado
    ON ordem_trabalho_patio (status_ordem, criado_em ASC);
