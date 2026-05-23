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
    ('navio/servicos', 'Serviços de Linha', 'navio/servicos', 'NAVIO', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 24, FALSE)
ON CONFLICT (identificador) DO NOTHING;
