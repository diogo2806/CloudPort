CREATE TABLE reserva_conteiner_carga_geral (
    id UUID PRIMARY KEY,
    unidade_id BIGINT NOT NULL,
    operacao_id UUID NOT NULL,
    status VARCHAR(20) NOT NULL,
    estado_anterior VARCHAR(30) NOT NULL,
    usuario_reserva VARCHAR(120) NOT NULL,
    reservado_em TIMESTAMP NOT NULL,
    usuario_liberacao VARCHAR(120),
    motivo_liberacao VARCHAR(1000),
    liberado_em TIMESTAMP,
    CONSTRAINT fk_reserva_conteiner_carga_geral_unidade
        FOREIGN KEY (unidade_id) REFERENCES unidade_inventario(id),
    CONSTRAINT uk_reserva_conteiner_carga_geral_operacao UNIQUE (operacao_id),
    CONSTRAINT ck_reserva_conteiner_carga_geral_status
        CHECK (status IN ('ATIVA', 'CONCLUIDA', 'CANCELADA'))
);

CREATE UNIQUE INDEX uk_reserva_conteiner_carga_geral_unidade_ativa
    ON reserva_conteiner_carga_geral(unidade_id)
    WHERE status = 'ATIVA';

CREATE INDEX idx_reserva_conteiner_carga_geral_status
    ON reserva_conteiner_carga_geral(status, reservado_em DESC);
