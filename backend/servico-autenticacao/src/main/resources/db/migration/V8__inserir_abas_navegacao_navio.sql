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
    ('navio/escalas', 'Cronograma de Escalas', 'navio/escalas', 'NAVIO', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 20, FALSE),
    ('navio/navios', 'Navios Cadastrados', 'navio/navios', 'NAVIO', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 21, FALSE)
ON CONFLICT (identificador) DO NOTHING;
