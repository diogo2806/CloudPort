-- Disponibiliza a aba "Planejamento de Embarque" (plano de estiva) no grupo NAVIO.
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
    ('embarque/planejamento', 'Planejamento de Embarque', 'embarque/planejamento', 'NAVIO', 'ROLE_ADMIN_PORTO,ROLE_PLANEJADOR', FALSE, NULL, 20, FALSE)
ON CONFLICT (identificador) DO NOTHING;
