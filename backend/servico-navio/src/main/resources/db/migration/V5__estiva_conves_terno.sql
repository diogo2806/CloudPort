-- Separa as camadas do plano em porão (in-hold) e convés (on-deck), classifica
-- cada atribuição pela localização e introduz o terno (crane split) que opera
-- um intervalo contíguo de baias.

-- Camadas por localização (substituem a contagem única de camadas).
ALTER TABLE plano_estiva ADD COLUMN IF NOT EXISTS camadas_porao INTEGER NOT NULL DEFAULT 1;
ALTER TABLE plano_estiva ADD COLUMN IF NOT EXISTS camadas_conves INTEGER NOT NULL DEFAULT 0;

UPDATE plano_estiva SET camadas_porao = camadas WHERE camadas IS NOT NULL;

ALTER TABLE plano_estiva ALTER COLUMN camadas_porao DROP DEFAULT;
ALTER TABLE plano_estiva ALTER COLUMN camadas_conves DROP DEFAULT;
ALTER TABLE plano_estiva DROP COLUMN IF EXISTS camadas;

-- Localização vertical da célula (derivada do número do tier).
ALTER TABLE atribuicao_estiva ADD COLUMN IF NOT EXISTS conves VARCHAR(10) NOT NULL DEFAULT 'PORAO';
UPDATE atribuicao_estiva SET conves = CASE WHEN camada >= 80 THEN 'CONVES' ELSE 'PORAO' END;
ALTER TABLE atribuicao_estiva ALTER COLUMN conves DROP DEFAULT;

-- Terno / crane split.
CREATE TABLE IF NOT EXISTS terno (
    id BIGSERIAL PRIMARY KEY,
    plano_id BIGINT NOT NULL REFERENCES plano_estiva (id),
    identificador VARCHAR(40) NOT NULL,
    sequencia INTEGER NOT NULL,
    baia_inicial INTEGER NOT NULL,
    baia_final INTEGER NOT NULL,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    CONSTRAINT uk_terno_identificador UNIQUE (plano_id, identificador)
);

CREATE INDEX IF NOT EXISTS idx_terno_plano ON terno (plano_id);
