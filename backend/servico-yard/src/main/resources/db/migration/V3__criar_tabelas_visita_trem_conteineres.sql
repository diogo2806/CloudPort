CREATE TABLE visita_trem_descarga (
    visita_trem_id BIGINT NOT NULL,
    id_conteiner BIGINT NOT NULL,
    status_operacao VARCHAR(20) NOT NULL,
    CONSTRAINT fk_visita_descarga_visita FOREIGN KEY (visita_trem_id)
        REFERENCES visita_trem (id) ON DELETE CASCADE
);

CREATE TABLE visita_trem_carga (
    visita_trem_id BIGINT NOT NULL,
    id_conteiner BIGINT NOT NULL,
    status_operacao VARCHAR(20) NOT NULL,
    CONSTRAINT fk_visita_carga_visita FOREIGN KEY (visita_trem_id)
        REFERENCES visita_trem (id) ON DELETE CASCADE
);

CREATE UNIQUE INDEX uk_visita_trem_descarga_conteiner
    ON visita_trem_descarga (visita_trem_id, id_conteiner);

CREATE UNIQUE INDEX uk_visita_trem_carga_conteiner
    ON visita_trem_carga (visita_trem_id, id_conteiner);
