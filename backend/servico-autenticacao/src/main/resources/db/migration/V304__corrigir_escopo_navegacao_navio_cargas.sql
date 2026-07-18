-- Explicita que contêineres pertencem ao fluxo principal do CloudPort e que
-- bobinas de aço são uma especialização de carga siderúrgica.
INSERT INTO configuracoes_navegacao (
    identificador,
    rotulo,
    rota,
    grupo,
    roles_permitidos,
    desabilitado,
    mensagem_em_breve,
    ordem,
    padrao
) VALUES
    (
        'navio/line-up',
        'Line-up de navios',
        'navio/line-up',
        'Navio e cargas',
        'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_GATE',
        FALSE,
        NULL,
        10,
        FALSE
    ),
    (
        'embarque/planejamento',
        'Estiva de contêineres',
        'embarque/planejamento',
        'Navio e cargas',
        'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR',
        FALSE,
        NULL,
        20,
        FALSE
    ),
    (
        'embarque/steel-coils',
        'Estiva de bobinas de aço',
        'embarque/steel-coils',
        'Navio e cargas',
        'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR',
        FALSE,
        NULL,
        22,
        FALSE
    )
ON CONFLICT (identificador) DO UPDATE
SET rotulo = EXCLUDED.rotulo,
    rota = EXCLUDED.rota,
    grupo = EXCLUDED.grupo,
    roles_permitidos = EXCLUDED.roles_permitidos,
    desabilitado = EXCLUDED.desabilitado,
    mensagem_em_breve = EXCLUDED.mensagem_em_breve,
    ordem = EXCLUDED.ordem,
    padrao = EXCLUDED.padrao;
