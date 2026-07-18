-- V110: operação persistida de tampas de porão integrada à execução dos guindastes

CREATE TABLE tampa_porao (
    id BIGSERIAL PRIMARY KEY,
    estivagem_plan_id BIGINT NOT NULL,
    codigo VARCHAR(40) NOT NULL,
    bay_inicial INTEGER NOT NULL,
    bay_final INTEGER NOT NULL,
    posicao VARCHAR(20) NOT NULL DEFAULT 'FECHADA',
    recurso_atual VARCHAR(80),
    versao_registro BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tampa_porao_plano
        FOREIGN KEY (estivagem_plan_id) REFERENCES estivagem_plan(id),
    CONSTRAINT uk_tampa_porao_plano_codigo
        UNIQUE (estivagem_plan_id, codigo),
    CONSTRAINT ck_tampa_porao_bays
        CHECK (bay_inicial > 0 AND bay_final >= bay_inicial),
    CONSTRAINT ck_tampa_porao_posicao
        CHECK (posicao IN ('FECHADA', 'ABERTA', 'REMOVIDA', 'POSICIONADA'))
);

CREATE TABLE tarefa_tampa_porao (
    id BIGSERIAL PRIMARY KEY,
    tampa_porao_id BIGINT NOT NULL,
    tipo VARCHAR(20) NOT NULL,
    status VARCHAR(20) NOT NULL DEFAULT 'PLANEJADA',
    ordem_operacional INTEGER NOT NULL,
    ordem_movimento_referencia INTEGER,
    momento_sequencia VARCHAR(10) NOT NULL,
    dependencia_id BIGINT,
    recurso VARCHAR(80),
    iniciado_por VARCHAR(120),
    confirmado_por VARCHAR(120),
    cancelado_por VARCHAR(120),
    observacao VARCHAR(500),
    iniciado_em TIMESTAMP,
    confirmado_em TIMESTAMP,
    cancelado_em TIMESTAMP,
    versao_registro BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tarefa_tampa_porao_tampa
        FOREIGN KEY (tampa_porao_id) REFERENCES tampa_porao(id),
    CONSTRAINT fk_tarefa_tampa_porao_dependencia
        FOREIGN KEY (dependencia_id) REFERENCES tarefa_tampa_porao(id),
    CONSTRAINT uk_tarefa_tampa_porao_ordem
        UNIQUE (tampa_porao_id, ordem_operacional),
    CONSTRAINT ck_tarefa_tampa_porao_tipo
        CHECK (tipo IN ('ABRIR', 'REMOVER', 'POSICIONAR', 'FECHAR')),
    CONSTRAINT ck_tarefa_tampa_porao_status
        CHECK (status IN ('PLANEJADA', 'LIBERADA', 'EM_EXECUCAO', 'CONCLUIDA', 'CANCELADA')),
    CONSTRAINT ck_tarefa_tampa_porao_momento
        CHECK (momento_sequencia IN ('ANTES', 'APOS')),
    CONSTRAINT ck_tarefa_tampa_porao_ordem
        CHECK (ordem_operacional BETWEEN 1 AND 4)
);

ALTER TABLE movimento_execucao_guindaste
    ADD COLUMN codigo_hatch_cover VARCHAR(40),
    ADD COLUMN sobre_hatch_cover BOOLEAN NOT NULL DEFAULT FALSE;

CREATE INDEX idx_tampa_porao_plano
    ON tampa_porao (estivagem_plan_id, bay_inicial, bay_final);

CREATE INDEX idx_tarefa_tampa_porao_status
    ON tarefa_tampa_porao (tampa_porao_id, status, ordem_operacional);

CREATE INDEX idx_movimento_execucao_hatch_cover
    ON movimento_execucao_guindaste (execucao_id, codigo_hatch_cover, status);
