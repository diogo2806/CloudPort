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
    ('patio/recursos', 'Gestão de Recursos', 'patio/recursos', 'PATIO', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR,ROLE_OPERADOR_PATIO', FALSE, NULL, 21, FALSE)
ON CONFLICT (identificador) DO NOTHING;
