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
        'billing',
        'Faturamento',
        'billing',
        'FATURAMENTO',
        'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR',
        FALSE,
        NULL,
        80,
        FALSE
    ),
    (
        'cap',
        'Portal da Transportadora',
        'cap',
        'PORTAL DO CLIENTE',
        'ROLE_TRANSPORTADORA',
        FALSE,
        NULL,
        81,
        FALSE
    )
ON CONFLICT (identificador) DO UPDATE SET
    rotulo = EXCLUDED.rotulo,
    rota = EXCLUDED.rota,
    grupo = EXCLUDED.grupo,
    roles_permitidos = EXCLUDED.roles_permitidos,
    desabilitado = EXCLUDED.desabilitado,
    mensagem_em_breve = EXCLUDED.mensagem_em_breve,
    ordem = EXCLUDED.ordem;
