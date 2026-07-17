CREATE TABLE IF NOT EXISTS transferencia_locomotiva (
    id BIGSERIAL PRIMARY KEY,
    visita_trem_id BIGINT NOT NULL,
    identificador_locomotiva VARCHAR(60) NOT NULL,
    operadora_ferroviaria VARCHAR(80) NOT NULL,
    fabricante VARCHAR(80),
    modelo VARCHAR(80),
    numero_serie VARCHAR(80),
    peso_toneladas NUMERIC(12, 3) NOT NULL,
    comprimento_metros NUMERIC(10, 3) NOT NULL,
    largura_metros NUMERIC(10, 3) NOT NULL,
    altura_metros NUMERIC(10, 3) NOT NULL,
    status VARCHAR(40) NOT NULL,
    nome_maquinista VARCHAR(120),
    documento_entrega VARCHAR(80),
    responsavel_terminal VARCHAR(120),
    entregue_em TIMESTAMP WITHOUT TIME ZONE,
    visita_navio_id BIGINT,
    codigo_visita_navio VARCHAR(60),
    modalidade_embarque VARCHAR(40),
    deck_planejado VARCHAR(80),
    posicao_planejada VARCHAR(120),
    freio_estacionamento_aplicado BOOLEAN NOT NULL DEFAULT FALSE,
    baterias_isoladas BOOLEAN NOT NULL DEFAULT FALSE,
    combustivel_protegido BOOLEAN NOT NULL DEFAULT FALSE,
    calcos_instalados BOOLEAN NOT NULL DEFAULT FALSE,
    plano_amarracao_aprovado BOOLEAN NOT NULL DEFAULT FALSE,
    liberada_em TIMESTAMP WITHOUT TIME ZONE,
    embarcada_em TIMESTAMP WITHOUT TIME ZONE,
    posicao_real VARCHAR(120),
    observacoes VARCHAR(1000),
    versao BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_transferencia_locomotiva_visita_trem
        FOREIGN KEY (visita_trem_id) REFERENCES visita_trem (id),
    CONSTRAINT uk_transferencia_locomotiva_visita_identificador
        UNIQUE (visita_trem_id, identificador_locomotiva),
    CONSTRAINT ck_transferencia_locomotiva_dimensoes
        CHECK (peso_toneladas > 0
            AND comprimento_metros > 0
            AND largura_metros > 0
            AND altura_metros > 0)
);

CREATE INDEX IF NOT EXISTS idx_transferencia_locomotiva_status
    ON transferencia_locomotiva (status);

CREATE INDEX IF NOT EXISTS idx_transferencia_locomotiva_navio
    ON transferencia_locomotiva (visita_navio_id, codigo_visita_navio);
