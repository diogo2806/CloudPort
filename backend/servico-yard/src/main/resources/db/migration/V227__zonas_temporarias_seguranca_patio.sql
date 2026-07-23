CREATE TABLE IF NOT EXISTS zona_seguranca_patio (
    id BIGSERIAL PRIMARY KEY,
    chave_idempotencia VARCHAR(120) NOT NULL UNIQUE,
    nome VARCHAR(120) NOT NULL,
    geometria TEXT,
    posicoes TEXT NOT NULL,
    inicio TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    fim TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    responsavel VARCHAR(120) NOT NULL,
    equipe VARCHAR(500) NOT NULL,
    motivo VARCHAR(1000) NOT NULL,
    estado VARCHAR(30) NOT NULL,
    bloqueia_origem BOOLEAN NOT NULL DEFAULT TRUE,
    bloqueia_destino BOOLEAN NOT NULL DEFAULT TRUE,
    bloqueia_rota BOOLEAN NOT NULL DEFAULT TRUE,
    versao BIGINT NOT NULL DEFAULT 1,
    ativada_em TIMESTAMP WITHOUT TIME ZONE,
    liberada_em TIMESTAMP WITHOUT TIME ZONE,
    liberada_por VARCHAR(120),
    motivo_liberacao VARCHAR(1000),
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_zona_seguranca_estado CHECK (estado IN ('RASCUNHO','ATIVA','EXPIRADA','LIBERADA')),
    CONSTRAINT ck_zona_seguranca_periodo CHECK (fim > inicio)
);

CREATE INDEX IF NOT EXISTS idx_zona_seguranca_vigencia
    ON zona_seguranca_patio (estado, inicio, fim);

CREATE TABLE IF NOT EXISTS zona_seguranca_patio_evento (
    id BIGSERIAL PRIMARY KEY,
    zona_id BIGINT NOT NULL REFERENCES zona_seguranca_patio(id),
    versao BIGINT NOT NULL,
    tipo VARCHAR(40) NOT NULL,
    payload TEXT NOT NULL,
    operador VARCHAR(120) NOT NULL,
    correlation_id VARCHAR(120),
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    UNIQUE (zona_id, versao)
);

CREATE TABLE IF NOT EXISTS zona_seguranca_patio_conflito (
    id BIGSERIAL PRIMARY KEY,
    zona_id BIGINT NOT NULL REFERENCES zona_seguranca_patio(id),
    ordem_trabalho_patio_id BIGINT NOT NULL REFERENCES ordem_trabalho_patio(id),
    posicao VARCHAR(120) NOT NULL,
    estado VARCHAR(30) NOT NULL DEFAULT 'ABERTO',
    detectado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolvido_em TIMESTAMP WITHOUT TIME ZONE,
    UNIQUE (zona_id, ordem_trabalho_patio_id, posicao)
);

CREATE OR REPLACE FUNCTION bloquear_dispatch_em_zona_seguranca()
RETURNS trigger AS $$
BEGIN
    IF NEW.status_ordem = 'EM_EXECUCAO' AND OLD.status_ordem IS DISTINCT FROM NEW.status_ordem THEN
        IF EXISTS (
            SELECT 1
              FROM zona_seguranca_patio z
             WHERE z.estado = 'ATIVA'
               AND CURRENT_TIMESTAMP BETWEEN z.inicio AND z.fim
               AND z.bloqueia_destino = TRUE
               AND ('|' || UPPER(z.posicoes) || '|') LIKE ('%|' || UPPER(NEW.destino) || '|%')
        ) THEN
            RAISE EXCEPTION 'Dispatch bloqueado: destino % pertence a zona temporária de segurança ativa.', NEW.destino
                USING ERRCODE = 'P0001';
        END IF;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_bloquear_dispatch_zona_seguranca ON ordem_trabalho_patio;
CREATE TRIGGER trg_bloquear_dispatch_zona_seguranca
BEFORE UPDATE OF status_ordem ON ordem_trabalho_patio
FOR EACH ROW EXECUTE FUNCTION bloquear_dispatch_em_zona_seguranca();