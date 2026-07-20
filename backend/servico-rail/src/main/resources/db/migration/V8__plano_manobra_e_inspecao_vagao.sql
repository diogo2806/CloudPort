CREATE EXTENSION IF NOT EXISTS btree_gist;

CREATE TABLE plano_manobra_ferroviaria (
    id BIGSERIAL PRIMARY KEY,
    visita_trem_id BIGINT NOT NULL,
    sequencia INTEGER NOT NULL,
    origem VARCHAR(120) NOT NULL,
    destino VARCHAR(120) NOT NULL,
    composicao VARCHAR(200) NOT NULL,
    linha VARCHAR(80) NOT NULL,
    trecho VARCHAR(120) NOT NULL,
    inicio_previsto TIMESTAMP NOT NULL,
    fim_previsto TIMESTAMP NOT NULL,
    status VARCHAR(30) NOT NULL,
    conflito_descricao VARCHAR(500),
    autorizado_por VARCHAR(120),
    autorizado_em TIMESTAMP,
    iniciado_em TIMESTAMP,
    concluido_em TIMESTAMP,
    motivo_cancelamento VARCHAR(500),
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_plano_manobra_visita
        FOREIGN KEY (visita_trem_id) REFERENCES visita_trem (id),
    CONSTRAINT uk_plano_manobra_visita_sequencia
        UNIQUE (visita_trem_id, sequencia),
    CONSTRAINT ck_plano_manobra_sequencia
        CHECK (sequencia > 0),
    CONSTRAINT ck_plano_manobra_periodo
        CHECK (fim_previsto > inicio_previsto),
    CONSTRAINT ck_plano_manobra_status
        CHECK (status IN ('PLANEJADA', 'BLOQUEADA_CONFLITO', 'AUTORIZADA', 'EM_EXECUCAO', 'CONCLUIDA', 'CANCELADA'))
);

ALTER TABLE plano_manobra_ferroviaria
    ADD CONSTRAINT ex_plano_manobra_reserva_trecho
    EXCLUDE USING gist (
        linha WITH =,
        trecho WITH =,
        tsrange(inicio_previsto, fim_previsto, '[)') WITH &&
    )
    WHERE (status IN ('PLANEJADA', 'AUTORIZADA', 'EM_EXECUCAO'));

CREATE INDEX idx_plano_manobra_visita_status
    ON plano_manobra_ferroviaria (visita_trem_id, status, sequencia);

CREATE TABLE inspecao_vagao (
    id BIGSERIAL PRIMARY KEY,
    visita_trem_id BIGINT NOT NULL,
    identificador_vagao VARCHAR(35) NOT NULL,
    status VARCHAR(30) NOT NULL,
    rodas_aprovadas BOOLEAN NOT NULL,
    freios_aprovados BOOLEAN NOT NULL,
    engates_aprovados BOOLEAN NOT NULL,
    estrutura_aprovada BOOLEAN NOT NULL,
    lacres_aprovados BOOLEAN NOT NULL,
    responsavel VARCHAR(120) NOT NULL,
    observacao VARCHAR(1000),
    inspecionado_em TIMESTAMP NOT NULL,
    override_por VARCHAR(120),
    override_motivo VARCHAR(500),
    liberado_em TIMESTAMP,
    criado_em TIMESTAMP NOT NULL,
    atualizado_em TIMESTAMP NOT NULL,
    versao BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT fk_inspecao_vagao_visita
        FOREIGN KEY (visita_trem_id) REFERENCES visita_trem (id),
    CONSTRAINT ck_inspecao_vagao_status
        CHECK (status IN ('APROVADA', 'REPROVADA', 'LIBERADA_OVERRIDE'))
);

CREATE INDEX idx_inspecao_vagao_ultima
    ON inspecao_vagao (visita_trem_id, identificador_vagao, inspecionado_em DESC);

CREATE TABLE defeito_inspecao_vagao (
    inspecao_vagao_id BIGINT NOT NULL,
    ordem_defeito INTEGER NOT NULL,
    codigo VARCHAR(40) NOT NULL,
    descricao VARCHAR(500) NOT NULL,
    severidade VARCHAR(20) NOT NULL,
    evidencia VARCHAR(500),
    PRIMARY KEY (inspecao_vagao_id, ordem_defeito),
    CONSTRAINT fk_defeito_inspecao
        FOREIGN KEY (inspecao_vagao_id) REFERENCES inspecao_vagao (id) ON DELETE CASCADE,
    CONSTRAINT ck_defeito_inspecao_severidade
        CHECK (severidade IN ('BAIXA', 'MEDIA', 'ALTA', 'CRITICA'))
);
