-- V6: Tabelas para planejamento de estivagem de granéis siderúrgicos (bobinas)

CREATE TABLE IF NOT EXISTS navio_granel (
    id                  BIGSERIAL PRIMARY KEY,
    imo                 VARCHAR(10),
    nome                VARCHAR(100) NOT NULL,
    classe              VARCHAR(20),
    lpp                 DOUBLE PRECISION,
    boca                DOUBLE PRECISION,
    calado              DOUBLE PRECISION,
    deslocamento        DOUBLE PRECISION,
    gm                  DOUBLE PRECISION DEFAULT 1.5,
    bm_max_permitido    DOUBLE PRECISION,
    sf_max_permitido    DOUBLE PRECISION,
    is_template         BOOLEAN NOT NULL DEFAULT FALSE,
    criado_em           TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_navio_granel_imo  ON navio_granel (imo);
CREATE INDEX IF NOT EXISTS idx_navio_granel_tmpl ON navio_granel (is_template);

CREATE TABLE IF NOT EXISTS porao_navio (
    id                  BIGSERIAL PRIMARY KEY,
    navio_granel_id     BIGINT NOT NULL REFERENCES navio_granel(id) ON DELETE CASCADE,
    numero              INTEGER NOT NULL,
    comprimento         DOUBLE PRECISION,
    largura             DOUBLE PRECISION,
    altura_util         DOUBLE PRECISION,
    area_util_m2        DOUBLE PRECISION,
    angulo_antepara     DOUBLE PRECISION,
    pos_long_inicio_m   DOUBLE PRECISION,
    pos_long_fim_m      DOUBLE PRECISION
);

CREATE INDEX IF NOT EXISTS idx_porao_navio_navio ON porao_navio (navio_granel_id);

CREATE TABLE IF NOT EXISTS setor_tanktop (
    id                  BIGSERIAL PRIMARY KEY,
    porao_id            BIGINT NOT NULL REFERENCES porao_navio(id) ON DELETE CASCADE,
    nome                VARCHAR(20),
    capacidade_t_m2     DOUBLE PRECISION NOT NULL,
    area_m2             DOUBLE PRECISION,
    pos_long_inicio     DOUBLE PRECISION,
    pos_long_fim        DOUBLE PRECISION,
    pos_trans_inicio    DOUBLE PRECISION,
    pos_trans_fim       DOUBLE PRECISION
);

CREATE INDEX IF NOT EXISTS idx_setor_tanktop_porao ON setor_tanktop (porao_id);

CREATE TABLE IF NOT EXISTS plano_estiva_bulk (
    id                  BIGSERIAL PRIMARY KEY,
    navio_granel_id     BIGINT REFERENCES navio_granel(id),
    codigo_viagem       VARCHAR(30),
    porto_carga         VARCHAR(10),
    porto_descarga      VARCHAR(10),
    status              VARCHAR(25) NOT NULL DEFAULT 'RASCUNHO',
    bm_max_calculado    DOUBLE PRECISION,
    sf_max_calculado    DOUBLE PRECISION,
    trim_calculado      DOUBLE PRECISION,
    list_calculado      DOUBLE PRECISION,
    calado_saida        DOUBLE PRECISION,
    versao              BIGINT NOT NULL DEFAULT 0,
    criado_em           TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_plano_estiva_navio  ON plano_estiva_bulk (navio_granel_id);
CREATE INDEX IF NOT EXISTS idx_plano_estiva_status ON plano_estiva_bulk (status);

CREATE TABLE IF NOT EXISTS bobina_manifesto (
    id                  BIGSERIAL PRIMARY KEY,
    plano_estiva_id     BIGINT NOT NULL REFERENCES plano_estiva_bulk(id) ON DELETE CASCADE,
    codigo              VARCHAR(30) NOT NULL,
    peso_kg             DOUBLE PRECISION,
    diametro_externo_mm DOUBLE PRECISION,
    diametro_interno_mm DOUBLE PRECISION,
    largura_mm          DOUBLE PRECISION,
    grau_aco            VARCHAR(20),
    porto_descarga      VARCHAR(10)
);

CREATE INDEX IF NOT EXISTS idx_bobina_plano ON bobina_manifesto (plano_estiva_id);

CREATE TABLE IF NOT EXISTS posicao_bobina (
    id                  BIGSERIAL PRIMARY KEY,
    plano_estiva_id     BIGINT NOT NULL REFERENCES plano_estiva_bulk(id) ON DELETE CASCADE,
    bobina_id           BIGINT NOT NULL REFERENCES bobina_manifesto(id),
    porao_id            BIGINT REFERENCES porao_navio(id),
    setor_id            BIGINT REFERENCES setor_tanktop(id),
    camada              INTEGER NOT NULL DEFAULT 1,
    posicao_x           DOUBLE PRECISION,
    posicao_y           DOUBLE PRECISION,
    angulo_inclinacao   DOUBLE PRECISION DEFAULT 0,
    espessura_dunnage_mm DOUBLE PRECISION DEFAULT 50.0,
    tipo_lashing        VARCHAR(25) DEFAULT 'SEM_LASHING',
    alerta_tanktop      VARCHAR(200),
    criado_em           TIMESTAMP NOT NULL DEFAULT NOW(),
    atualizado_em       TIMESTAMP NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_posicao_bobina_plano  ON posicao_bobina (plano_estiva_id);
CREATE INDEX IF NOT EXISTS idx_posicao_bobina_porao  ON posicao_bobina (porao_id);

CREATE TABLE IF NOT EXISTS material_lashing_bulk (
    id                  BIGSERIAL PRIMARY KEY,
    plano_estiva_id     BIGINT NOT NULL REFERENCES plano_estiva_bulk(id) ON DELETE CASCADE,
    tipo                VARCHAR(25),
    quantidade          INTEGER,
    comprimento_m       DOUBLE PRECISION,
    peso_unitario_kg    DOUBLE PRECISION,
    descricao           VARCHAR(200)
);

CREATE INDEX IF NOT EXISTS idx_lashing_plano ON material_lashing_bulk (plano_estiva_id);
