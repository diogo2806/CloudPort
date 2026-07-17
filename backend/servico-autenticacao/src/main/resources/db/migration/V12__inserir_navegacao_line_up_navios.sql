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
    ('navio/line-up', 'Line-up de Navios', 'navio/line-up', 'NAVIO', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_GATE', FALSE, NULL, 10, FALSE)
ON CONFLICT (identificador) DO NOTHING;
