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
    ('patio/dispatch', 'Dispatch de Movimentações', 'patio/dispatch', 'PATIO', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_PATIO', FALSE, NULL, 23, FALSE)
ON CONFLICT (identificador) DO NOTHING;
