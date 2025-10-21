CREATE TABLE visita_trem_vagao (
    visita_trem_id BIGINT NOT NULL,
    posicao_no_trem INTEGER NOT NULL,
    identificador_vagao VARCHAR(35) NOT NULL,
    tipo_vagao VARCHAR(40),
    ordem_vagao INTEGER NOT NULL,
    CONSTRAINT fk_vagao_visita FOREIGN KEY (visita_trem_id)
        REFERENCES visita_trem (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_visita_trem_vagao_identificador
    ON visita_trem_vagao (visita_trem_id, identificador_vagao);
