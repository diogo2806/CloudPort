CREATE TABLE IF NOT EXISTS aviso_estivagem_patio (
    id BIGSERIAL PRIMARY KEY,
    chave_estavel VARCHAR(220) NOT NULL,
    codigo_unidade VARCHAR(40) NOT NULL,
    posicao_id BIGINT NOT NULL,
    bloco VARCHAR(40),
    linha INTEGER NOT NULL,
    coluna INTEGER NOT NULL,
    camada VARCHAR(40) NOT NULL,
    regra VARCHAR(40) NOT NULL,
    severidade VARCHAR(20) NOT NULL,
    status VARCHAR(40) NOT NULL,
    descricao VARCHAR(1000) NOT NULL,
    valor_observado VARCHAR(1000),
    valor_esperado VARCHAR(1000),
    acao_sugerida VARCHAR(1000),
    responsavel VARCHAR(120),
    prazo TIMESTAMP WITHOUT TIME ZONE,
    acao_corretiva VARCHAR(2000),
    evidencia VARCHAR(2000),
    resultado VARCHAR(2000),
    aberto_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    ultima_revalidacao_em TIMESTAMP WITHOUT TIME ZONE NOT NULL,
    resolvido_em TIMESTAMP WITHOUT TIME ZONE,
    recorrencias INTEGER NOT NULL DEFAULT 0,
    versao BIGINT NOT NULL DEFAULT 0,
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    atualizado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_aviso_estivagem_chave_estavel UNIQUE (chave_estavel),
    CONSTRAINT ck_aviso_estivagem_regra CHECK (regra IN (
        'PESO','ALTURA','TIPO_CARGA','REEFER','PERIGOSO','CAPACIDADE','RESERVA','APOIO','REGRA_PILHA')),
    CONSTRAINT ck_aviso_estivagem_severidade CHECK (severidade IN ('CRITICA','ALTA','MEDIA','BAIXA')),
    CONSTRAINT ck_aviso_estivagem_status CHECK (status IN (
        'ABERTO','ATRIBUIDO','EM_CORRECAO','AGUARDANDO_REVALIDACAO','RESOLVIDO','REABERTO')),
    CONSTRAINT ck_aviso_estivagem_recorrencias CHECK (recorrencias >= 0)
);

CREATE INDEX IF NOT EXISTS idx_aviso_estivagem_fila
    ON aviso_estivagem_patio (status, severidade, prazo, atualizado_em DESC);

CREATE INDEX IF NOT EXISTS idx_aviso_estivagem_posicao
    ON aviso_estivagem_patio (linha, coluna, camada, status);

CREATE INDEX IF NOT EXISTS idx_aviso_estivagem_unidade
    ON aviso_estivagem_patio (UPPER(codigo_unidade), status);

CREATE TABLE IF NOT EXISTS historico_aviso_estivagem_patio (
    id BIGSERIAL PRIMARY KEY,
    aviso_id BIGINT NOT NULL REFERENCES aviso_estivagem_patio(id) ON DELETE CASCADE,
    tipo_evento VARCHAR(40) NOT NULL,
    status_anterior VARCHAR(40),
    status_novo VARCHAR(40) NOT NULL,
    ator VARCHAR(120) NOT NULL,
    detalhes VARCHAR(2000),
    evidencia VARCHAR(2000),
    resultado VARCHAR(2000),
    criado_em TIMESTAMP WITHOUT TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_historico_aviso_estivagem_evento CHECK (tipo_evento IN (
        'ABERTURA','ATRIBUICAO','CORRECAO_INICIADA','ENVIO_REVALIDACAO',
        'REVALIDACAO_FALHOU','RESOLUCAO','REABERTURA','ATUALIZACAO_AUTOMATICA')),
    CONSTRAINT ck_historico_aviso_estivagem_status_anterior CHECK (
        status_anterior IS NULL OR status_anterior IN (
            'ABERTO','ATRIBUIDO','EM_CORRECAO','AGUARDANDO_REVALIDACAO','RESOLVIDO','REABERTO')),
    CONSTRAINT ck_historico_aviso_estivagem_status_novo CHECK (status_novo IN (
        'ABERTO','ATRIBUIDO','EM_CORRECAO','AGUARDANDO_REVALIDACAO','RESOLVIDO','REABERTO'))
);

CREATE INDEX IF NOT EXISTS idx_historico_aviso_estivagem
    ON historico_aviso_estivagem_patio (aviso_id, criado_em, id);

CREATE OR REPLACE FUNCTION bloquear_dispatch_por_aviso_estivagem_critico()
RETURNS trigger AS $$
BEGIN
    IF NEW.status_ordem = 'EM_EXECUCAO'
       AND OLD.status_ordem IS DISTINCT FROM NEW.status_ordem
       AND EXISTS (
            SELECT 1
              FROM aviso_estivagem_patio aviso
             WHERE aviso.severidade = 'CRITICA'
               AND aviso.status IN ('ABERTO','ATRIBUIDO','EM_CORRECAO','AGUARDANDO_REVALIDACAO','REABERTO')
               AND aviso.linha = NEW.linha_destino
               AND aviso.coluna = NEW.coluna_destino
               AND UPPER(aviso.camada) = UPPER(NEW.camada_destino)
       ) THEN
        RAISE EXCEPTION 'Dispatch bloqueado: existe aviso crítico de estivagem ativo na posição L%/C%/%',
            NEW.linha_destino, NEW.coluna_destino, NEW.camada_destino
            USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_bloquear_dispatch_aviso_estivagem ON ordem_trabalho_patio;
CREATE TRIGGER trg_bloquear_dispatch_aviso_estivagem
BEFORE UPDATE OF status_ordem ON ordem_trabalho_patio
FOR EACH ROW EXECUTE FUNCTION bloquear_dispatch_por_aviso_estivagem_critico();

CREATE OR REPLACE FUNCTION bloquear_plano_por_aviso_estivagem_critico()
RETURNS trigger AS $$
BEGIN
    IF NEW.estado IN ('TENTATIVO','DEFINITIVO','IMINENTE')
       AND EXISTS (
            SELECT 1
              FROM aviso_estivagem_patio aviso
             WHERE aviso.severidade = 'CRITICA'
               AND aviso.status IN ('ABERTO','ATRIBUIDO','EM_CORRECAO','AGUARDANDO_REVALIDACAO','REABERTO')
               AND aviso.linha = NEW.linha
               AND aviso.coluna = NEW.coluna
               AND UPPER(aviso.camada) = UPPER(NEW.camada)
       ) THEN
        RAISE EXCEPTION 'Plano bloqueado: existe aviso crítico de estivagem ativo na posição L%/C%/%',
            NEW.linha, NEW.coluna, NEW.camada
            USING ERRCODE = 'P0001';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

DROP TRIGGER IF EXISTS trg_bloquear_plano_aviso_estivagem ON plano_posicao_operacional;
CREATE TRIGGER trg_bloquear_plano_aviso_estivagem
BEFORE INSERT OR UPDATE OF linha, coluna, camada, estado ON plano_posicao_operacional
FOR EACH ROW EXECUTE FUNCTION bloquear_plano_por_aviso_estivagem_critico();
