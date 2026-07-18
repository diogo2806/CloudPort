ALTER TABLE visita_trem_descarga
    ADD COLUMN IF NOT EXISTS identificador_vagao VARCHAR(35);

ALTER TABLE visita_trem_carga
    ADD COLUMN IF NOT EXISTS identificador_vagao VARCHAR(35);

ALTER TABLE visita_trem_vagao
    DROP CONSTRAINT IF EXISTS ck_visita_trem_vagao_capacidade_conteineres;

ALTER TABLE visita_trem_vagao
    ADD CONSTRAINT ck_visita_trem_vagao_capacidade_conteineres
        CHECK (capacidade_conteineres > 0);

ALTER TABLE replanejamento_conteiner_ferroviario
    ADD CONSTRAINT ck_replanejamento_posicao_origem
        CHECK (posicao_origem > 0);

ALTER TABLE replanejamento_conteiner_ferroviario
    ADD CONSTRAINT ck_replanejamento_posicao_destino
        CHECK (posicao_destino > 0);

ALTER TABLE replanejamento_conteiner_ferroviario
    ADD CONSTRAINT ck_replanejamento_ordem_origem
        CHECK (ordem_manifesto_origem > 0);

ALTER TABLE replanejamento_conteiner_ferroviario
    ADD CONSTRAINT ck_replanejamento_ordem_destino
        CHECK (ordem_manifesto_destino > 0);

ALTER TABLE replanejamento_conteiner_ferroviario
    ADD CONSTRAINT ck_replanejamento_vagoes_distintos
        CHECK (vagao_origem <> vagao_destino);

CREATE INDEX IF NOT EXISTS idx_ordem_movimentacao_replanejamento
    ON ordem_movimentacao (visita_trem_id, codigo_conteiner, tipo_movimentacao);
