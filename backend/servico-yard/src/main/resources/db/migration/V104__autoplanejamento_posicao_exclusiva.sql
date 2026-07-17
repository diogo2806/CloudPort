ALTER TABLE conteiner_patio
    ALTER COLUMN posicao_id DROP NOT NULL;

WITH ocupacoes_duplicadas AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY posicao_id
               ORDER BY atualizado_em DESC, id DESC
           ) AS ordem_ocupacao
    FROM conteiner_patio
    WHERE posicao_id IS NOT NULL
      AND status_conteiner NOT IN ('LIBERADO', 'DESPACHADO')
)
UPDATE conteiner_patio conteiner
SET posicao_id = NULL
FROM ocupacoes_duplicadas duplicada
WHERE conteiner.id = duplicada.id
  AND duplicada.ordem_ocupacao > 1;

CREATE UNIQUE INDEX IF NOT EXISTS uk_conteiner_patio_posicao_ativa
    ON conteiner_patio (posicao_id)
    WHERE posicao_id IS NOT NULL
      AND status_conteiner NOT IN ('LIBERADO', 'DESPACHADO');
