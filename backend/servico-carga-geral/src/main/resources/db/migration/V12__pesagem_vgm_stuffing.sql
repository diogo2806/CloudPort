ALTER TABLE operacao_stuff_unstuff
    ADD COLUMN metodo_pesagem_vgm VARCHAR(20),
    ADD COLUMN status_pesagem_vgm VARCHAR(30) NOT NULL DEFAULT 'PENDENTE',
    ADD COLUMN tara_kg NUMERIC(19,3),
    ADD COLUMN peso_bruto_kg NUMERIC(19,3),
    ADD COLUMN vgm_kg NUMERIC(19,3),
    ADD COLUMN capacidade_maxima_kg NUMERIC(19,3),
    ADD COLUMN equipamento_pesagem VARCHAR(120),
    ADD COLUMN responsavel_pesagem VARCHAR(120),
    ADD COLUMN pesagem_confirmada_em TIMESTAMP WITH TIME ZONE,
    ADD COLUMN motivo_bloqueio_peso VARCHAR(1000);

ALTER TABLE operacao_stuff_unstuff
    ADD CONSTRAINT ck_operacao_stuff_unstuff_metodo_pesagem
        CHECK (metodo_pesagem_vgm IS NULL OR metodo_pesagem_vgm IN ('METODO_1', 'METODO_2')),
    ADD CONSTRAINT ck_operacao_stuff_unstuff_status_pesagem
        CHECK (status_pesagem_vgm IN ('PENDENTE', 'CONFIRMADA', 'BLOQUEADA_EXCESSO')),
    ADD CONSTRAINT ck_operacao_stuff_unstuff_valores_pesagem
        CHECK (
            (tara_kg IS NULL OR tara_kg > 0)
            AND (peso_bruto_kg IS NULL OR peso_bruto_kg > 0)
            AND (vgm_kg IS NULL OR vgm_kg > 0)
            AND (capacidade_maxima_kg IS NULL OR capacidade_maxima_kg > 0)
        ),
    ADD CONSTRAINT ck_operacao_stuff_unstuff_pesagem_confirmada
        CHECK (
            status_pesagem_vgm = 'PENDENTE'
            OR (
                tipo = 'STUFF'
                AND metodo_pesagem_vgm IS NOT NULL
                AND tara_kg IS NOT NULL
                AND peso_bruto_kg IS NOT NULL
                AND vgm_kg IS NOT NULL
                AND capacidade_maxima_kg IS NOT NULL
                AND equipamento_pesagem IS NOT NULL
                AND responsavel_pesagem IS NOT NULL
                AND pesagem_confirmada_em IS NOT NULL
            )
        ),
    ADD CONSTRAINT ck_operacao_stuff_unstuff_bloqueio_peso
        CHECK (
            (status_pesagem_vgm = 'BLOQUEADA_EXCESSO' AND motivo_bloqueio_peso IS NOT NULL)
            OR (status_pesagem_vgm <> 'BLOQUEADA_EXCESSO' AND motivo_bloqueio_peso IS NULL)
        );

CREATE INDEX idx_operacao_stuff_unstuff_pesagem
    ON operacao_stuff_unstuff(status_pesagem_vgm, pesagem_confirmada_em DESC);
