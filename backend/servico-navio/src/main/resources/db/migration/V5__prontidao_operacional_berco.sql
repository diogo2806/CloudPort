-- BUS1350: prontidão bloqueante do berço antes do início da operação

CREATE TABLE IF NOT EXISTS prontidao_berco (
    id BIGSERIAL PRIMARY KEY,
    escala_id BIGINT NOT NULL,
    versao_checklist INTEGER NOT NULL,
    berco VARCHAR(40) NOT NULL,
    calado_metros NUMERIC(10, 3) NOT NULL,
    berco_confirmado BOOLEAN NOT NULL,
    calado_confirmado BOOLEAN NOT NULL,
    defensas_confirmadas BOOLEAN NOT NULL,
    amarracao_confirmada BOOLEAN NOT NULL,
    acesso_confirmado BOOLEAN NOT NULL,
    recursos_confirmados BOOLEAN NOT NULL,
    restricoes_avaliadas BOOLEAN NOT NULL,
    liberacoes_confirmadas BOOLEAN NOT NULL,
    recursos VARCHAR(1000),
    restricoes VARCHAR(1000),
    liberacoes VARCHAR(1000),
    observacoes VARCHAR(1000),
    responsavel VARCHAR(120) NOT NULL,
    confirmado_em TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_prontidao_berco_escala
        FOREIGN KEY (escala_id) REFERENCES escala (id) ON DELETE CASCADE,
    CONSTRAINT uk_prontidao_berco_escala_versao
        UNIQUE (escala_id, versao_checklist),
    CONSTRAINT ck_prontidao_berco_versao
        CHECK (versao_checklist > 0),
    CONSTRAINT ck_prontidao_berco_calado
        CHECK (calado_metros >= 0)
);

CREATE INDEX IF NOT EXISTS idx_prontidao_berco_escala_confirmado
    ON prontidao_berco (escala_id, confirmado_em DESC);
