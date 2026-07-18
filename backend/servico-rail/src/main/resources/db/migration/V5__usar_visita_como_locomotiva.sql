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

CREATE OR REPLACE FUNCTION validar_visita_locomotiva_sem_composicao()
RETURNS TRIGGER AS $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM visita_trem
        WHERE id = NEW.visita_trem_id
          AND tipo_visita = 'LOCOMOTIVA_ISOLADA'
    ) THEN
        RAISE EXCEPTION 'A locomotiva é a própria visita e não pode possuir vagões ou contêineres.'
            USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_bloquear_vagao_locomotiva ON visita_trem_vagao;
CREATE TRIGGER trg_bloquear_vagao_locomotiva
BEFORE INSERT OR UPDATE OF visita_trem_id ON visita_trem_vagao
FOR EACH ROW EXECUTE FUNCTION validar_visita_locomotiva_sem_composicao();

DROP TRIGGER IF EXISTS trg_bloquear_carga_locomotiva ON visita_trem_carga;
CREATE TRIGGER trg_bloquear_carga_locomotiva
BEFORE INSERT OR UPDATE OF visita_trem_id ON visita_trem_carga
FOR EACH ROW EXECUTE FUNCTION validar_visita_locomotiva_sem_composicao();

DROP TRIGGER IF EXISTS trg_bloquear_descarga_locomotiva ON visita_trem_descarga;
CREATE TRIGGER trg_bloquear_descarga_locomotiva
BEFORE INSERT OR UPDATE OF visita_trem_id ON visita_trem_descarga
FOR EACH ROW EXECUTE FUNCTION validar_visita_locomotiva_sem_composicao();
