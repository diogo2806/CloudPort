ALTER TABLE evento_vmt_work_instruction
    ADD COLUMN tipo_acao_fisica VARCHAR(30),
    ADD COLUMN codigo_unidade_lido VARCHAR(40),
    ADD COLUMN equipamento_patio_id BIGINT REFERENCES equipamento_patio(id),
    ADD COLUMN equipamento_identificador VARCHAR(80),
    ADD COLUMN origem_fisica VARCHAR(120),
    ADD COLUMN destino_fisico VARCHAR(120),
    ADD COLUMN linha_origem INTEGER,
    ADD COLUMN coluna_origem INTEGER,
    ADD COLUMN camada_origem VARCHAR(40),
    ADD COLUMN linha_destino INTEGER,
    ADD COLUMN coluna_destino INTEGER,
    ADD COLUMN camada_destino VARCHAR(40),
    ADD COLUMN sequencia_operacional INTEGER;

ALTER TABLE evento_vmt_work_instruction
    ADD CONSTRAINT ck_evento_vmt_tipo_acao_fisica CHECK (
        tipo_acao_fisica IS NULL OR tipo_acao_fisica IN ('GROUNDING', 'UNGROUNDING')
    );

CREATE INDEX idx_evento_vmt_transferencia_fisica
    ON evento_vmt_work_instruction (
        ordem_trabalho_patio_id,
        tipo_acao_fisica,
        ocorrido_em
    );
