ALTER TABLE yard_position_divergence
    ADD COLUMN condicao_anterior VARCHAR(30) NOT NULL DEFAULT 'OPERACIONAL';

ALTER TABLE yard_position_divergence
    ADD CONSTRAINT ck_yard_position_divergence_previous_condition CHECK (
        condicao_anterior IN (
            'OPERACIONAL',
            'AVARIADO',
            'INOPERANTE',
            'EM_INSPECAO',
            'EM_REPARO',
            'AGUARDANDO_PECA'
        )
    );

ALTER TABLE yard_position_divergence
    ALTER COLUMN condicao_anterior DROP DEFAULT;
