-- V116: operação persistida de tampas de porão vinculada ao Vessel Planner

CREATE TABLE tampa_porao (
    id BIGSERIAL PRIMARY KEY,
    estivagem_plan_id BIGINT NOT NULL,
    codigo VARCHAR(40) NOT NULL,
    estado VARCHAR(20) NOT NULL DEFAULT 'FECHADA',
    versao BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tampa_porao_plano
        FOREIGN KEY (estivagem_plan_id) REFERENCES estivagem_plan(id),
    CONSTRAINT uk_tampa_porao_plano_codigo
        UNIQUE (estivagem_plan_id, codigo),
    CONSTRAINT ck_tampa_porao_estado
        CHECK (estado IN ('FECHADA', 'ABERTA', 'REMOVIDA', 'POSICIONADA'))
);

CREATE TABLE posicao_tampa_porao (
    id BIGSERIAL PRIMARY KEY,
    tampa_porao_id BIGINT NOT NULL,
    tipo VARCHAR(30) NOT NULL,
    referencia VARCHAR(120) NOT NULL,
    ativa BOOLEAN NOT NULL DEFAULT TRUE,
    inicio_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fim_em TIMESTAMP,
    CONSTRAINT fk_posicao_tampa_porao_tampa
        FOREIGN KEY (tampa_porao_id) REFERENCES tampa_porao(id),
    CONSTRAINT ck_posicao_tampa_porao_tipo
        CHECK (tipo IN ('SOBRE_PORAO', 'AREA_SEGURA', 'CONVES', 'CAIS', 'EQUIPAMENTO')),
    CONSTRAINT ck_posicao_tampa_porao_periodo
        CHECK (fim_em IS NULL OR fim_em >= inicio_em)
);

CREATE TABLE tarefa_tampa_porao (
    id BIGSERIAL PRIMARY KEY,
    tampa_porao_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANEJADA',
    recurso VARCHAR(120) NOT NULL,
    operador VARCHAR(120) NOT NULL,
    motivo VARCHAR(500),
    posicao_destino_tipo VARCHAR(30),
    posicao_destino_referencia VARCHAR(120),
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    iniciado_em TIMESTAMP,
    concluido_em TIMESTAMP,
    cancelado_em TIMESTAMP,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_tarefa_tampa_porao_tampa
        FOREIGN KEY (tampa_porao_id) REFERENCES tampa_porao(id),
    CONSTRAINT ck_tarefa_tampa_porao_tipo
        CHECK (tipo IN ('ABRIR', 'REMOVER', 'POSICIONAR', 'FECHAR')),
    CONSTRAINT ck_tarefa_tampa_porao_status
        CHECK (status IN ('PLANEJADA', 'EM_EXECUCAO', 'CONCLUIDA', 'CANCELADA')),
    CONSTRAINT ck_tarefa_tampa_porao_posicao
        CHECK (posicao_destino_tipo IS NULL OR posicao_destino_tipo IN
            ('SOBRE_PORAO', 'AREA_SEGURA', 'CONVES', 'CAIS', 'EQUIPAMENTO'))
);

CREATE TABLE dependencia_tarefa_tampa_porao (
    id BIGSERIAL PRIMARY KEY,
    tarefa_id BIGINT NOT NULL,
    dependencia_id BIGINT NOT NULL,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_dependencia_tampa_tarefa
        FOREIGN KEY (tarefa_id) REFERENCES tarefa_tampa_porao(id),
    CONSTRAINT fk_dependencia_tampa_predecessora
        FOREIGN KEY (dependencia_id) REFERENCES tarefa_tampa_porao(id),
    CONSTRAINT uk_dependencia_tarefa_tampa
        UNIQUE (tarefa_id, dependencia_id),
    CONSTRAINT ck_dependencia_tarefa_tampa_distinta
        CHECK (tarefa_id <> dependencia_id)
);

CREATE INDEX idx_tampa_porao_plano_estado
    ON tampa_porao (estivagem_plan_id, estado);

CREATE INDEX idx_posicao_tampa_porao_ativa
    ON posicao_tampa_porao (tampa_porao_id, ativa);

CREATE UNIQUE INDEX uk_posicao_tampa_porao_ativa
    ON posicao_tampa_porao (tampa_porao_id)
    WHERE ativa = TRUE;

CREATE INDEX idx_tarefa_tampa_porao_status
    ON tarefa_tampa_porao (tampa_porao_id, status, criado_em);

CREATE INDEX idx_dependencia_tarefa_tampa_tarefa
    ON dependencia_tarefa_tampa_porao (tarefa_id);

INSERT INTO tampa_porao (estivagem_plan_id, codigo, estado, versao, criado_em, atualizado_em)
SELECT DISTINCT
       slot.estivagem_plan_id,
       UPPER(TRIM(slot.codigo_hatch_cover)),
       'FECHADA',
       0,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP
  FROM slot_navio slot
 WHERE slot.codigo_hatch_cover IS NOT NULL
   AND TRIM(slot.codigo_hatch_cover) <> ''
ON CONFLICT (estivagem_plan_id, codigo) DO NOTHING;

INSERT INTO posicao_tampa_porao (tampa_porao_id, tipo, referencia, ativa, inicio_em)
SELECT tampa.id,
       'SOBRE_PORAO',
       tampa.codigo,
       TRUE,
       CURRENT_TIMESTAMP
  FROM tampa_porao tampa
 WHERE NOT EXISTS (
       SELECT 1
         FROM posicao_tampa_porao posicao
        WHERE posicao.tampa_porao_id = tampa.id
          AND posicao.ativa = TRUE
 );
