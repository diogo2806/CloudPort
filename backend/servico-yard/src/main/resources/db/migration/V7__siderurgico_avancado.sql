-- V7: Expansão do planner siderúrgico — multi-produto, ballast, porto de viagem

CREATE TABLE IF NOT EXISTS tanque_ballast (
    id                      BIGSERIAL PRIMARY KEY,
    navio_granel_id         BIGINT NOT NULL REFERENCES navio_granel(id) ON DELETE CASCADE,
    nome                    VARCHAR(30) NOT NULL,
    capacidade_m3           DOUBLE PRECISION NOT NULL,
    volume_atual_m3         DOUBLE PRECISION DEFAULT 0.0,
    pos_long_centro_m       DOUBLE PRECISION,
    pos_trans_centro_m      DOUBLE PRECISION,
    vcg_cheio_m             DOUBLE PRECISION
);

CREATE INDEX IF NOT EXISTS idx_tanque_navio ON tanque_ballast (navio_granel_id);

CREATE TABLE IF NOT EXISTS porto_viagem (
    id                      BIGSERIAL PRIMARY KEY,
    plano_estiva_id         BIGINT NOT NULL REFERENCES plano_estiva_bulk(id) ON DELETE CASCADE,
    codigo_porto            VARCHAR(10) NOT NULL,
    nome_porto              VARCHAR(100),
    sequencia               INTEGER NOT NULL,
    tipo_operacao           VARCHAR(20) DEFAULT 'DESCARGA',
    calado_maximo_m         DOUBLE PRECISION,
    restricao_aire_m        DOUBLE PRECISION
);

CREATE INDEX IF NOT EXISTS idx_porto_viagem_plano ON porto_viagem (plano_estiva_id);

CREATE TABLE IF NOT EXISTS item_cargo_siderurgico (
    id                      BIGSERIAL PRIMARY KEY,
    plano_estiva_id         BIGINT NOT NULL REFERENCES plano_estiva_bulk(id) ON DELETE CASCADE,
    tipo_carga              VARCHAR(20) NOT NULL,
    codigo                  VARCHAR(40) NOT NULL,
    heat_number             VARCHAR(20),
    ordem_venda_erp         VARCHAR(20),
    numero_corrida          VARCHAR(20),
    peso_kg                 DOUBLE PRECISION,
    comprimento_mm          DOUBLE PRECISION,
    largura_mm              DOUBLE PRECISION,
    altura_mm               DOUBLE PRECISION,
    diametro_externo_mm     DOUBLE PRECISION,
    grau_aco                VARCHAR(20),
    porto_descarga          VARCHAR(10),
    requer_berceiro         BOOLEAN DEFAULT FALSE,
    requer_reforco_piso     BOOLEAN DEFAULT FALSE,
    max_camadas_empilhamento INTEGER
);

CREATE INDEX IF NOT EXISTS idx_item_cargo_plano  ON item_cargo_siderurgico (plano_estiva_id);
CREATE INDEX IF NOT EXISTS idx_item_cargo_tipo   ON item_cargo_siderurgico (tipo_carga);
CREATE INDEX IF NOT EXISTS idx_item_cargo_porto  ON item_cargo_siderurgico (porto_descarga);

-- Extend plano_estiva_bulk with three-point draft columns
ALTER TABLE plano_estiva_bulk
    ADD COLUMN IF NOT EXISTS calado_proa_m  DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS calado_meio_m  DOUBLE PRECISION,
    ADD COLUMN IF NOT EXISTS calado_popa_m  DOUBLE PRECISION;
