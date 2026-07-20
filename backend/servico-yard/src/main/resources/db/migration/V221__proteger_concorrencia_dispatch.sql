CREATE UNIQUE INDEX uk_decisao_dispatch_ordem_ativa
    ON decisao_dispatch (ordem_trabalho_patio_id)
    WHERE status IN ('RECOMENDADA', 'ATRIBUIDA');

CREATE INDEX idx_gatilho_dispatch_processado_ordem
    ON gatilho_dispatch_processado (ordem_trabalho_patio_id, processado_em DESC);
