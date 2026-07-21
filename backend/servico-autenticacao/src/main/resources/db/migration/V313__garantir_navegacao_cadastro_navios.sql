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
) VALUES (
    'navio/cadastros',
    'Navios',
    'navio/cadastros',
    'CADASTROS',
    'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR',
    FALSE,
    NULL,
    8,
    FALSE
)
ON CONFLICT (identificador) DO UPDATE SET
    rotulo = EXCLUDED.rotulo,
    rota = EXCLUDED.rota,
    grupo = EXCLUDED.grupo,
    roles_permitidos = EXCLUDED.roles_permitidos,
    desabilitado = FALSE,
    mensagem_em_breve = NULL,
    ordem = EXCLUDED.ordem;
