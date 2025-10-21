CREATE TABLE visita_trem_descarga (
    visita_trem_id BIGINT NOT NULL,
    codigo_conteiner VARCHAR(20) NOT NULL,
    status_operacao VARCHAR(20) NOT NULL,
    ordem_manifesto_descarga INTEGER NOT NULL,
    CONSTRAINT fk_descarga_visita FOREIGN KEY (visita_trem_id)
        REFERENCES visita_trem (id) ON DELETE CASCADE
);

CREATE TABLE visita_trem_carga (
    visita_trem_id BIGINT NOT NULL,
    codigo_conteiner VARCHAR(20) NOT NULL,
    status_operacao VARCHAR(20) NOT NULL,
    ordem_manifesto_carga INTEGER NOT NULL,
    CONSTRAINT fk_carga_visita FOREIGN KEY (visita_trem_id)
        REFERENCES visita_trem (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_visita_trem_descarga_codigo
    ON visita_trem_descarga (visita_trem_id, codigo_conteiner);

CREATE UNIQUE INDEX uk_visita_trem_carga_codigo
    ON visita_trem_carga (visita_trem_id, codigo_conteiner);
