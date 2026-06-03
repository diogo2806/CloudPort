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
    ('embarque/steel-coils', 'Steel Coil Planner', 'embarque/steel-coils', 'NAVIO', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 22, FALSE)
ON CONFLICT (identificador) DO NOTHING;
