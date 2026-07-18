ALTER TABLE visita_trem
    ADD COLUMN IF NOT EXISTS tipo_visita VARCHAR(30);

UPDATE visita_trem
SET tipo_visita = 'COMPOSICAO_FERROVIARIA'
WHERE tipo_visita IS NULL;

UPDATE visita_trem visita
SET tipo_visita = 'LOCOMOTIVA_ISOLADA'
WHERE EXISTS (
    SELECT 1
    FROM transferencia_locomotiva transferencia
    WHERE transferencia.visita_trem_id = visita.id
);

ALTER TABLE visita_trem
    ALTER COLUMN tipo_visita SET DEFAULT 'COMPOSICAO_FERROVIARIA';

ALTER TABLE visita_trem
    ALTER COLUMN tipo_visita SET NOT NULL;

WITH registros_duplicados AS (
    SELECT id,
           ROW_NUMBER() OVER (
               PARTITION BY visita_trem_id
               ORDER BY atualizado_em DESC, id DESC
           ) AS ordem
    FROM transferencia_locomotiva
)
DELETE FROM transferencia_locomotiva transferencia
USING registros_duplicados duplicado
WHERE transferencia.id = duplicado.id
  AND duplicado.ordem > 1;

ALTER TABLE transferencia_locomotiva
    DROP CONSTRAINT IF EXISTS uk_transferencia_locomotiva_visita_identificador;

ALTER TABLE transferencia_locomotiva
    DROP CONSTRAINT IF EXISTS transferencia_locomotiva_pkey;

ALTER TABLE transferencia_locomotiva
    DROP COLUMN IF EXISTS id;

ALTER TABLE transferencia_locomotiva
    DROP COLUMN IF EXISTS identificador_locomotiva;

ALTER TABLE transferencia_locomotiva
    DROP COLUMN IF EXISTS operadora_ferroviaria;

ALTER TABLE transferencia_locomotiva
    ADD CONSTRAINT transferencia_locomotiva_pkey PRIMARY KEY (visita_trem_id);
