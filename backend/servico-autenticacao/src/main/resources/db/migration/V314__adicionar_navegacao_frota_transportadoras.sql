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
        'cadastro-frota-transportadoras',
        'Veículos e carretas de transportadoras',
        'cadastros/frota',
        'CADASTROS',
        'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_GATE',
        FALSE,
        NULL,
        16,
        FALSE
    ),
    (
        'minha-frota-transportadora',
        'Minha frota',
        'cap/frota',
        'PORTAL DO CLIENTE',
        'ROLE_TRANSPORTADORA',
        FALSE,
        NULL,
        82,
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
