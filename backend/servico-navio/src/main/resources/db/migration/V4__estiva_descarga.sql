-- Habilita o planejamento de descarga (navio -> pátio) reutilizando o plano de
-- estiva: cada atribuição passa a ter um tipo de operação (EMBARQUE/DESCARGA) e
-- uma posição de destino no pátio (usada na descarga, que dá entrada no estoque).

ALTER TABLE atribuicao_estiva
    ADD COLUMN IF NOT EXISTS tipo_operacao VARCHAR(20) NOT NULL DEFAULT 'EMBARQUE';

ALTER TABLE atribuicao_estiva
    ADD COLUMN IF NOT EXISTS posicao_patio_destino VARCHAR(40);

-- A célula passa a ser única por operação, permitindo planejar a descarga e o
-- (re)embarque da mesma posição física dentro de uma escala.
ALTER TABLE atribuicao_estiva DROP CONSTRAINT IF EXISTS uk_atribuicao_celula;
ALTER TABLE atribuicao_estiva
    ADD CONSTRAINT uk_atribuicao_celula UNIQUE (plano_id, tipo_operacao, baia, fileira, camada);

-- O valor padrão foi usado apenas para preencher as linhas existentes; a
-- aplicação sempre informa o tipo explicitamente.
ALTER TABLE atribuicao_estiva ALTER COLUMN tipo_operacao DROP DEFAULT;

CREATE INDEX IF NOT EXISTS idx_atribuicao_estiva_operacao ON atribuicao_estiva (tipo_operacao);
