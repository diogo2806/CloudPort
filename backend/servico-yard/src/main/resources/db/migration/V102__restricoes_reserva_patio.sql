ALTER TABLE posicao_patio ADD COLUMN IF NOT EXISTS bloco VARCHAR(40);
ALTER TABLE posicao_patio ADD COLUMN IF NOT EXISTS bloqueada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE posicao_patio ADD COLUMN IF NOT EXISTS interditada BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE posicao_patio ADD COLUMN IF NOT EXISTS area_permitida BOOLEAN NOT NULL DEFAULT TRUE;
ALTER TABLE posicao_patio ADD COLUMN IF NOT EXISTS tipos_carga_permitidos VARCHAR(500);
ALTER TABLE posicao_patio ADD COLUMN IF NOT EXISTS peso_maximo_toneladas NUMERIC(12, 3);
ALTER TABLE posicao_patio ADD COLUMN IF NOT EXISTS altura_maxima_metros NUMERIC(8, 3);
ALTER TABLE posicao_patio ADD COLUMN IF NOT EXISTS camada_maxima INTEGER;
ALTER TABLE posicao_patio ADD COLUMN IF NOT EXISTS capacidade_pilha INTEGER;

ALTER TABLE posicao_patio DROP CONSTRAINT IF EXISTS ck_posicao_patio_camada_maxima;
ALTER TABLE posicao_patio ADD CONSTRAINT ck_posicao_patio_camada_maxima
    CHECK (camada_maxima IS NULL OR camada_maxima > 0);

ALTER TABLE posicao_patio DROP CONSTRAINT IF EXISTS ck_posicao_patio_capacidade_pilha;
ALTER TABLE posicao_patio ADD CONSTRAINT ck_posicao_patio_capacidade_pilha
    CHECK (capacidade_pilha IS NULL OR capacidade_pilha > 0);

ALTER TABLE posicao_patio DROP CONSTRAINT IF EXISTS ck_posicao_patio_peso_maximo;
ALTER TABLE posicao_patio ADD CONSTRAINT ck_posicao_patio_peso_maximo
    CHECK (peso_maximo_toneladas IS NULL OR peso_maximo_toneladas > 0);

ALTER TABLE posicao_patio DROP CONSTRAINT IF EXISTS ck_posicao_patio_altura_maxima;
ALTER TABLE posicao_patio ADD CONSTRAINT ck_posicao_patio_altura_maxima
    CHECK (altura_maxima_metros IS NULL OR altura_maxima_metros > 0);

CREATE INDEX IF NOT EXISTS idx_posicao_patio_bloco ON posicao_patio (bloco);
CREATE INDEX IF NOT EXISTS idx_posicao_patio_disponibilidade
    ON posicao_patio (bloqueada, interditada, area_permitida);
