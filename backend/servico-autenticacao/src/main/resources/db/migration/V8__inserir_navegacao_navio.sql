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
    ('navio/visitas', 'Visitas de Navio', 'navio/visitas', 'NAVIO', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 20, FALSE),
    ('navio/painel', 'Painel de Atracação', 'navio/painel', 'NAVIO', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 21, FALSE),
    ('navio/bercos', 'Berços', 'navio/bercos', 'NAVIO', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 22, FALSE)
ON CONFLICT (identificador) DO NOTHING;
